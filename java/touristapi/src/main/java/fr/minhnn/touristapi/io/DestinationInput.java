package fr.minhnn.touristapi.io;

import fr.minhnn.touristapi.exceptions.BadRequestException;

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
            throw new BadRequestException("Name is required");
        }
        if (types == null || types.isEmpty()) {
            throw new BadRequestException("At least one destination type is required");
        }
        if (detailAddress == null || detailAddress.isBlank()) {
            throw new BadRequestException("Detail address is required");
        }
        if (city == null || city.isBlank()) {
            throw new BadRequestException("City is required");
        }
        if (country == null || country.isBlank()) {
            throw new BadRequestException("Country is required");
        }
    }
}
