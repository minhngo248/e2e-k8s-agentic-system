package fr.minhnn.touristapi.destination;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DestinationRepository {
    int save(Destination destination);
    Optional<Destination> findById(UUID id);
    List<Destination> findByTypeAndCoordinates(List<Destination.DestinationType> types, Double latitude, Double longitude, Double radiusKm);
    Optional<Destination> findByLatitudeAndLongitude(Double latitude, Double longitude);
    int update(Destination destination);
}
