package fr.minhnn.touristapi.destination;

import fr.minhnn.touristapi.exceptions.BadRequestException;

import java.util.List;

public class DestinationManagement {
    public static Destination createDestination(
            String name,
            String description,
            List<String> typeStrings,
            String detailAddress,
            String city,
            String postalCode,
            String region,
            String country,
            String countryCode
    ) {
        List<Destination.DestinationType> types = typeStrings.stream()
                .map(Destination.DestinationType::fromLabel)
                .toList();
        return new Destination(
                name,
                description,
                types,
                detailAddress,
                city,
                postalCode,
                region,
                country,
                countryCode
        );
    }

    public static Destination createDestinationWithCoordinates(
            String name,
            String description,
            List<String> typeStrings,
            String detailAddress,
            String city,
            String postalCode,
            String region,
            String country,
            String countryCode,
            Double latitude,
            Double longitude
    ) {
        List<Destination.DestinationType> types = typeStrings.stream()
                .map(Destination.DestinationType::fromLabel)
                .toList();
        return new Destination(
                name,
                description,
                types,
                detailAddress,
                city,
                postalCode,
                region,
                country,
                countryCode,
                latitude,
                longitude
        );
    }

    public static void addImages(Destination destination, List<String> urlImages) {
        destination.addImages(urlImages);
    }

    public static void ensureUniquenessAt(DestinationRepository repository, Double latitude,
                                   Double longitude, String name) {
        repository.findByLatitudeAndLongitude(latitude, longitude)
                .ifPresent(existing -> {
                    if (existing.getName().equalsIgnoreCase(name)) {
                        throw new BadRequestException("Destination already exists");
                    }
                });
    }

    public static void enrichWithLocation(Destination destination, Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new BadRequestException("Coordinates cannot be null");
        }
        if (latitude < -90.0 || latitude > 90.0) {
            throw new BadRequestException("Latitude must be between -90 and 90");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new BadRequestException("Longitude must be between -180 and 180");
        }
        destination.setCoordinates(latitude, longitude);
    }
}
