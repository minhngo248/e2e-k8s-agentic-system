package fr.minhnn.touristagent;

import java.util.List;

public record DestinationOutput(
        String id,
        String name,
        String description,
        List<String> types,
        String detailAddress,
        String city,
        String postalCode,
        String region,
        String country,
        String countryCode,
        Double latitude,
        Double longitude,
        List<String> imageUrls
) {
}
