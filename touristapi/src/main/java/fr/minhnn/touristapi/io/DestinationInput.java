package fr.minhnn.touristapi.io;

import java.util.List;

public record DestinationInput(
        String name,
        String description,
        List<String> types,
        String detailAddress,
        String city,
        String postalCode,
        String region,
        String country,
        String countryCode
) {
    public DestinationInput {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (types == null || types.isEmpty()) {
            throw new IllegalArgumentException("At least one destination type is required");
        }
        if (detailAddress == null || detailAddress.isBlank()) {
            throw new IllegalArgumentException("Detail address is required");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City is required");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Country is required");
        }
    }
}
