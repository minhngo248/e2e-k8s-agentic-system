package fr.minhnn.touristapi.infra;

import fr.minhnn.touristapi.destination.DestinationService;
import fr.minhnn.touristapi.io.DestinationInput;
import fr.minhnn.touristapi.io.DestinationOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Log4j2
public class DestinationController {
    private final DestinationService destinationService;

    @PostMapping(path = "/api/v1/destinations", consumes = "multipart/form-data")
    public ResponseEntity<DestinationOutput> saveDestination(
            @RequestPart("data") DestinationInput input,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        log.info("Received request to save destination: {} with {} images", input, images != null ? images.size() : 0);
        ResponseEntity<DestinationOutput> result = ResponseEntity.ok(destinationService.saveDestination(input, images));
        log.info("Saved destination: {}", result.getBody());
        return result;
    }

    @GetMapping("/api/v1/destinations")
    public ResponseEntity<List<DestinationOutput>> getDestinationsByTypesAndLocation(
            @RequestParam List<String> types,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Double radiusKm) {
        log.info("Received request to get destinations with types: {}, latitude: {}, longitude: {}, radiusKm: {}",
                types, latitude, longitude, radiusKm);
        ResponseEntity<List<DestinationOutput>> results = ResponseEntity.ok(destinationService.getDestinationsByTypesAndLocation(types, latitude, longitude, radiusKm));
        log.info("Found {} destinations matching criteria", results.getBody() != null ? results.getBody().size() : 0);
        return results;
    }
}
