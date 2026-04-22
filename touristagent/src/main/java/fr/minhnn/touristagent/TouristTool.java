package fr.minhnn.touristagent;

import fr.minhnn.touristagent.config.TouristApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
class TouristTool {
    private final RestClient restClient;
    private final TouristApiProperties touristApiProperties;

    /**
     * Find tourist destinations by types and location.
     * @param types a list of destination types (e.g., "Beach",
     *         "Mountain",
     *         "City",
     *         Historical",
     *         "Cultural",
     *         "Adventure",
     *         "Nature",
     *         "Relaxation",
     *         "Romantic",
     *         "Family",
     *         "Luxury",
     *         "Budget",
     *         "Rural",
     *         "Festival",
     *         "Wildlife",
     *         "Nightlife",
     *         "Foodie",
     *         "Desert",
     *         "Island",
     *         "Winter Sports",
     *         "Pilgrimage",
     *         "Eco-tourism",
     *         "Cruise",
     *         "School",
     *         "Indoor",
     *         "Outdoor")
     * @param latitude the latitude of the location
     * @param longitude the longitude of the location
     * @param radiusKm the search radius in kilometers
     * @return a list of DestinationOutput
     */
    @Tool
    public List<DestinationOutput> findDestinationsByTypesAndLocation(
            @ToolParam List<String> types,
            @ToolParam Double latitude,
            @ToolParam Double longitude,
            @ToolParam Double radiusKm) {
        return Arrays.asList(Objects.requireNonNull(restClient.get()
                .uri(touristApiProperties.getUrl(), builder -> builder
                        .path("/api/v1/destinations")
                        .queryParam("types", String.join(",", types))
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("radiusKm", radiusKm)
                        .build())
                .retrieve()
                .toEntity(DestinationOutput[].class).getBody()));
    }
}
