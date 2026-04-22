package fr.minhnn.touristapi.infra;

import fr.minhnn.touristapi.destination.DestinationService;
import fr.minhnn.touristapi.io.DestinationInput;
import fr.minhnn.touristapi.io.DestinationOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DestinationController {
    private final DestinationService destinationService;

    @PostMapping(path = "/api/v1/destinations", consumes = "multipart/form-data")
    public ResponseEntity<DestinationOutput> saveDestination(
            @RequestPart("data") DestinationInput input,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ResponseEntity.ok(destinationService.saveDestination(input, images));
    }

    @GetMapping("/api/v1/destinations")
    public ResponseEntity<List<DestinationOutput>> getDestinationsByTypesAndLocation(
            @RequestParam List<String> types,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Double radiusKm) {
        return ResponseEntity.ok(destinationService.getDestinationsByTypesAndLocation(types, latitude, longitude, radiusKm));
    }
}
