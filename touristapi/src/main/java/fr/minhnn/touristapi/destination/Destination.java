package fr.minhnn.touristapi.destination;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
public class Destination {
    private final DestinationIdentifier id;
    private String name;
    private String description;
    private List<DestinationType> types;
    private String detailAddress;
    private String city;
    private String postalCode;
    private String region;
    private String country;
    private String countryCode;
    private Coordinates coordinates;
    private List<String> urlImages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // constructor for creating new destinations
    Destination(String name,
                String description,
                List<DestinationType> types,
                String detailAddress,
                String city,
                String postalCode,
                String region,
                String country,
                String countryCode,
                Double latitude,
                Double longitude) {
        this.id = new DestinationIdentifier(UUID.randomUUID());
        this.name = name;
        this.description = description;
        this.types = types;
        this.detailAddress = detailAddress;
        this.city = city;
        this.postalCode = postalCode;
        this.region = region;
        this.country = country;
        this.countryCode = countryCode;
        this.coordinates = new Coordinates(latitude, longitude);
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // constructor for creating new destinations
    Destination(String name,
                String description,
                List<DestinationType> types,
                String detailAddress,
                String city,
                String postalCode,
                String region,
                String country,
                String countryCode) {
        this.id = new DestinationIdentifier(UUID.randomUUID());
        this.name = name;
        this.description = description;
        this.types = types;
        this.detailAddress = detailAddress;
        this.city = city;
        this.postalCode = postalCode;
        this.region = region;
        this.country = country;
        this.countryCode = countryCode;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    void addImages(List<String> urlImages) {
        this.urlImages = urlImages;
        this.updatedAt = LocalDateTime.now();
    }

    void setCoordinates(Double latitude, Double longitude) {
        this.coordinates = new Coordinates(latitude, longitude);
        this.updatedAt = LocalDateTime.now();
    }

    public record DestinationIdentifier(UUID id) { }

    public enum DestinationType {
        BEACH("Beach"),
        MOUNTAIN("Mountain"),
        CITY("City"),
        HISTORICAL("Historical"),
        CULTURAL("Cultural"),
        ADVENTURE("Adventure"),
        NATURE("Nature"),
        RELAXATION("Relaxation"),
        ROMANTIC("Romantic"),
        FAMILY("Family"),
        LUXURY("Luxury"),
        BUDGET("Budget"),
        RURAL("Rural"),
        FESTIVAL("Festival"),
        WILDLIFE("Wildlife"),
        NIGHTLIFE("Nightlife"),
        FOODIE("Foodie"),
        DESERT("Desert"),
        ISLAND("Island"),
        WINTER_SPORTS("Winter Sports"),
        PILGRIMAGE("Pilgrimage"),
        ECO_TOURISM("Eco-tourism"),
        CRUISE("Cruise"),
        SCHOOL("School"),
        INDOOR("Indoor"),
        OUTDOOR("Outdoor");

        private final String label;

        DestinationType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static DestinationType fromLabel(String label) {
            for (DestinationType type : values()) {
                if (type.label.equalsIgnoreCase(label)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown DestinationType label: " + label);
        }
    }

    public record Coordinates(Double latitude, Double longitude) {}

    public record ImageFile(String fileName, byte[] content, String contentType) {
        public ImageFile {
            Objects.requireNonNull(fileName, "fileName cannot be null");
            Objects.requireNonNull(content, "content cannot be null");
            Objects.requireNonNull(contentType, "contentType cannot be null");
        }
    }
}
