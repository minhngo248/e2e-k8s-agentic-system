package fr.minhnn.touristapi.io;

import java.util.List;

public record SearchDestinationInput(
        List<String> types,
        Double latitude,
        Double longitude,
        Double radiusKm
) {
}
