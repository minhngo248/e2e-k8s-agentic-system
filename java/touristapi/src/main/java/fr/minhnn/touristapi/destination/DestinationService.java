package fr.minhnn.touristapi.destination;

import fr.minhnn.touristapi.exceptions.BadRequestException;
import fr.minhnn.touristapi.io.DestinationInput;
import fr.minhnn.touristapi.io.DestinationOutput;
import fr.minhnn.touristapi.infra.MultipartFileAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DestinationService {
    private final DestinationRepository destinationRepository;
    private final S3Service s3Service;
    private final GoogleMapService googleMapService;

    @Transactional
    public DestinationOutput saveDestination(DestinationInput input, List<MultipartFile> images) {
        Destination destination = DestinationManagement.createDestination(
                input.name(),
                input.description(),
                input.types(),
                input.detailAddress(),
                input.city(),
                input.postalCode(),
                input.region(),
                input.country(),
                input.countryCode()
        );
        String text = String.join(", ",
                destination.getDetailAddress(),
                destination.getCity(),
                destination.getCountry()
        );
        Destination.Coordinates coordinates = googleMapService.geocode(text);
        DestinationManagement.ensureUniquenessAt(destinationRepository, coordinates.latitude(),
                coordinates.longitude(), destination.getName());
        DestinationManagement.enrichWithLocation(destination, coordinates.latitude(), coordinates.longitude());
        destinationRepository.save(destination);
        // Upload images to S3 with destination ID
        List<Destination.ImageFile> imageFiles = new ArrayList<>();
        images.parallelStream()
                .forEach(image -> {
                    synchronized (imageFiles) {
                        try {
                            imageFiles.add(
                                    MultipartFileAdapter.toDomain(image)
                            );
                        } catch (IOException e) {
                            throw new BadRequestException("Failed to process image: " + image.getOriginalFilename());
                        }
                    }
                });
        List<String> imageUrls = s3Service.uploadImages(imageFiles, "destinations/" + destination.getId().id());
        DestinationManagement.addImages(destination, imageUrls);
        destinationRepository.update(destination);
        return new DestinationOutput(
                destination.getId().id().toString(),
                destination.getName(),
                destination.getDescription(),
                destination.getTypes().stream().map(Destination.DestinationType::getLabel).toList(),
                destination.getDetailAddress(),
                destination.getCity(),
                destination.getPostalCode(),
                destination.getRegion(),
                destination.getCountry(),
                destination.getCountryCode(),
                destination.getCoordinates().latitude(),
                destination.getCoordinates().longitude(),
                destination.getUrlImages()
        );
    }

    public List<DestinationOutput> getDestinationsByTypesAndLocation(List<String> typeStrings, Double latitude, Double longitude, Double radiusKm) {
        List<Destination.DestinationType> types = typeStrings.stream()
                .map(Destination.DestinationType::fromLabel)
                .toList();
        List<Destination> destinations = destinationRepository.findByTypeAndCoordinates(types, latitude, longitude, radiusKm);
        return destinations.stream()
                .map(dest -> new DestinationOutput(
                        dest.getId().id().toString(),
                        dest.getName(),
                        dest.getDescription(),
                        dest.getTypes().stream().map(Destination.DestinationType::getLabel).toList(),
                        dest.getDetailAddress(),
                        dest.getCity(),
                        dest.getPostalCode(),
                        dest.getRegion(),
                        dest.getCountry(),
                        dest.getCountryCode(),
                        dest.getCoordinates().latitude(),
                        dest.getCoordinates().longitude(),
                        dest.getUrlImages()
                ))
                .toList();
    }
}
