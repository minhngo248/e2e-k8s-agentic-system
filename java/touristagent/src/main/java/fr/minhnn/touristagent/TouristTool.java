package fr.minhnn.touristagent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class TouristTool {
    private final RestClient restClient;
    private final TouristApiProperties touristApiProperties;

    public TouristTool(RestClient restClient, TouristApiProperties touristApiProperties) {
        this.restClient = restClient;
        this.touristApiProperties = touristApiProperties;
    }

    /**
     * Find tourist destinations by types and location.
     * @param types a list of destination types (Here are existing types: "Beach",
     *         "Mountain",
     *         "City",
     *         "Historical",
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
    @Tool(description = "Find tourist destinations by types and location")
    public List<DestinationOutput> findDestinationsByTypesAndLocation(
            @ToolParam(description = "A list of destination types. Existing types: 'Beach', 'Mountain', 'City', 'Historical', 'Cultural', 'Adventure', 'Nature', 'Relaxation', 'Romantic', 'Family', 'Luxury', 'Budget', 'Rural', 'Festival', 'Wildlife', 'Nightlife', 'Foodie', 'Desert', 'Island', 'Winter Sports', 'Pilgrimage', 'Eco-tourism', 'Cruise', 'School', 'Indoor', 'Outdoor'.")
                List<String> types,
            @ToolParam(description = "The latitude of the location") Double latitude,
            @ToolParam(description = "The longitude of the location") Double longitude,
            @ToolParam(description = "The search radius in kilometers") Double radiusKm) {
        log.info("Received request to find destinations by types: {}, latitude: {}, longitude: {}, radiusKm: {}", types, latitude, longitude, radiusKm);
        List<DestinationOutput> results = Arrays.asList(Objects.requireNonNull(restClient.get()
                .uri(touristApiProperties.getUrl(), builder -> builder
                        .path("/api/v1/destinations")
                        .queryParam("types", String.join(",", types))
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("radiusKm", radiusKm)
                        .build())
                .retrieve()
                .toEntity(DestinationOutput[].class).getBody()));
        log.info("Found {} destinations matching criteria", results.size());
        return results;
    }
}
