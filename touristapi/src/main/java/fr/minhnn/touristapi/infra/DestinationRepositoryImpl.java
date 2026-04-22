package fr.minhnn.touristapi.infra;

import fr.minhnn.touristapi.destination.Destination;
import fr.minhnn.touristapi.destination.DestinationManagement;
import fr.minhnn.touristapi.destination.DestinationRepository;
import fr.minhnn.touristapi.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DestinationRepositoryImpl implements DestinationRepository {
    private final JdbcClient jdbcClient;
    private final JsonMapper jsonMapper;

    @Override
    public int save(Destination destination) {
        String sql = "INSERT INTO destinations (id, created_at, updated_at, " +
                "name, description, types, detail_address, city, " +
                "postal_code, region, country, country_code, latitude, longitude, url_images) " +
                "VALUES (:id, :created_at, :updated_at, :name, " +
                ":description, :types::jsonb, :detail_address, :city, :postal_code, " +
                ":region, :country, :country_code, :latitude, :longitude, :url_images::jsonb)";
        return jdbcClient.sql(sql)
                .param("id", destination.getId().id())
                .param("created_at", destination.getCreatedAt())
                .param("updated_at", destination.getUpdatedAt())
                .param("name", destination.getName())
                .param("description", destination.getDescription())
                .param("types", convertDestinationTypesToJson(destination.getTypes()))
                .param("detail_address", destination.getDetailAddress())
                .param("city", destination.getCity())
                .param("postal_code", destination.getPostalCode())
                .param("region", destination.getRegion())
                .param("country", destination.getCountry())
                .param("country_code", destination.getCountryCode())
                .param("latitude", destination.getCoordinates().latitude())
                .param("longitude", destination.getCoordinates().longitude())
                .param("url_images", convertUrlImagesToJson(destination.getUrlImages()))
                .update();
    }

    @Override
    public Optional<Destination> findById(UUID id) {
        String sql = "SELECT * FROM destinations WHERE id = ?";
        return jdbcClient.sql(sql)
                .param(id)
                .query(rs -> {
                            if (rs.next()) {
                                return Optional.of(mapRowToDestination(rs));
                            } else {
                                return Optional.empty();
                            }
                        }
                );
    }

    @Override
    public List<Destination> findByTypeAndCoordinates(@NonNull List<Destination.DestinationType> types,
                                                      @NonNull Double latitude,
                                                      @NonNull Double longitude,
                                                      @NonNull Double radiusKm) {
        StringBuilder sql = new StringBuilder("SELECT * FROM destinations WHERE ");
        List<Object> params = new ArrayList<>();

        if (!types.isEmpty()) {
            sql.append("types @> ?::jsonb ");
            params.add(convertDestinationTypesToJson(types));
        }
        sql.append(" AND (6371 * acos(cos(radians(?)) * cos(radians(latitude)) * " +
                "cos(radians(longitude) - radians(?)) + sin(radians(?)) * sin(radians(latitude)))) <= ?");
        params.add(latitude);
        params.add(longitude);
        params.add(latitude);
        params.add(radiusKm);

        return jdbcClient.sql(sql.toString())
                .params(params.toArray())
                .query(rs -> {
                            List<Destination> destinations = new ArrayList<>();
                            while (rs.next()) {
                                destinations.add(mapRowToDestination(rs));
                            }
                            return destinations;
                        }
                );
    }

    @Override
    public Optional<Destination> findByLatitudeAndLongitude(Double latitude, Double longitude) {
        String sql = "SELECT * FROM destinations WHERE latitude = ? AND longitude = ?";
        return jdbcClient.sql(sql)
                .param(latitude)
                .param(longitude)
                .query(rs -> {
                            if (rs.next()) {
                                return Optional.of(mapRowToDestination(rs));
                            } else {
                                return Optional.empty();
                            }
                        }
                );
    }

    @Override
    public int update(Destination destination) {
        String sql = "UPDATE destinations SET updated_at = ?, url_images = ?::jsonb, " +
                "longitude = ?, latitude = ? WHERE id = ?";

        return jdbcClient.sql(sql)
                .param(destination.getUpdatedAt())
                .param(convertUrlImagesToJson(destination.getUrlImages()))
                .param(destination.getCoordinates().longitude())
                .param(destination.getCoordinates().latitude())
                .param(destination.getId().id())
                .update();
    }

    private @Nullable String convertUrlImagesToJson(List<String> urlImages) {
        try {
            return jsonMapper.writeValueAsString(urlImages);
        } catch (JacksonException e) {
            throw new BadRequestException("Failed to convert list to JSON");
        }
    }

    private @Nullable String convertDestinationTypesToJson(List<Destination.DestinationType> types) {
        try {
            List<String> typeNames = types.stream()
                    .map(Enum::name)
                    .collect(Collectors.toList());
            return jsonMapper.writeValueAsString(typeNames);
        } catch (JacksonException e) {
            throw new BadRequestException("Failed to convert types to JSON");
        }
    }

    // RowMapper for mapping ResultSet to Destination entity
    private Destination mapRowToDestination(ResultSet rs) {
        try {
            Destination destination = DestinationManagement.createDestinationWithCoordinates(
                    rs.getString("name"),
                    rs.getString("description"),
                    parseTypesFromJson(rs.getString("types")).stream().map(Destination.DestinationType::toString).toList(),
                    rs.getString("detail_address"),
                    rs.getString("city"),
                    rs.getString("postal_code"),
                    rs.getString("region"),
                    rs.getString("country"),
                    rs.getString("country_code"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude")
            );
            DestinationManagement.addImages(destination, parseUrlImagesFromJson(rs.getString("url_images")));
            return destination;
        } catch (Exception e) {
            throw new BadRequestException("Failed to map ResultSet to Destination");
        }
    }

    private List<String> parseUrlImagesFromJson(String urlImages) {
        if (urlImages == null || urlImages.isEmpty()) {
            return List.of();
        }
        try {
            return jsonMapper.readValue(urlImages, new TypeReference<List<String>>() {});
        } catch (JacksonException e) {
            throw new BadRequestException("Failed to convert image URLs JSON to a list");
        }
    }

    private List<Destination.DestinationType> parseTypesFromJson(String json) {
        if (json == null || json.trim().isEmpty() || json.equals("[]")) {
            return new ArrayList<>();
        }
        try {
            List<String> typeNames = jsonMapper.readValue(json, new TypeReference<List<String>>() {});
            return typeNames.stream()
                    .map(Destination.DestinationType::valueOf)
                    .collect(Collectors.toList());
        } catch (JacksonException e) {
            throw new BadRequestException("Failed to parse destination types from JSON");
        }
    }
}
