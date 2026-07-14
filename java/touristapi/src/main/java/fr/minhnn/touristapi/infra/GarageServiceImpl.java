package fr.minhnn.touristapi.infra;

import fr.minhnn.touristapi.config.S3Properties;
import fr.minhnn.touristapi.destination.Destination;
import fr.minhnn.touristapi.destination.S3Service;
import fr.minhnn.touristapi.exceptions.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Profile("garage")
public class GarageServiceImpl implements S3Service {
    private final S3Properties s3Properties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofMinutes(15);
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L;

    public GarageServiceImpl(S3Properties s3Properties,
                             @Qualifier("garageS3Client") S3Client s3Client,
                             @Qualifier("garageS3Presigner") S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.s3Properties = s3Properties;
    }

    @Override
    public List<String> uploadImages(List<Destination.ImageFile> files, String folder) {
        List<MultipartFile> multipartFiles = new ArrayList<>();
        files.parallelStream().forEach(file -> {
            log.debug("Preparing to upload image: {}", file.fileName());
            synchronized (multipartFiles) {
                multipartFiles.add(MultipartFileAdapter.toMultipartFile(file));
            }
        });

        validateImages(multipartFiles);

        List<String> uploadedUrls = new ArrayList<>();
        multipartFiles.parallelStream().forEach(multipartFile -> {
            try {
                String url = uploadSingleImage(multipartFile, folder);
                synchronized (uploadedUrls) {
                    uploadedUrls.add(url);
                }
            } catch (Exception e) {
                log.error("Failed to upload image: {}", multipartFile.getOriginalFilename(), e);
                throw new BadRequestException("Failed to upload image: " + multipartFile.getOriginalFilename());
            }
        });

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteImages(uploadedUrls);
                }
            }
        });

        return uploadedUrls;
    }

    private String uploadSingleImage(MultipartFile file, String folder) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String fileName = UUID.randomUUID() + extension;
        String key = folder + "/" + fileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        String url = String.format("%s/%s/%s",
                s3Properties.getEndpoint(),
                s3Properties.getBucketName(),
                key);

        log.info("Uploaded image to Garage: {}", url);
        return url;
    }

    @Override
    public void deleteImage(String imageUrl) {
        try {
            String key = extractKeyFromUrl(imageUrl);
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Deleted image from Garage: {}", imageUrl);
        } catch (Exception e) {
            log.error("Failed to delete image from Garage: {}", imageUrl, e);
        }
    }

    @Override
    public void deleteImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        imageUrls.forEach(this::deleteImage);
    }

    @Override
    public String generatePresignedImageUrl(String imageUrlOrKey) {
        String key = extractKeyFromUrl(imageUrlOrKey);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(key)
                .build();

        PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(request ->
                request.signatureDuration(PRESIGNED_URL_EXPIRATION)
                        .getObjectRequest(getObjectRequest)
        );

        return presignedGetObjectRequest.url().toString();
    }

    private void validateImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("At least 1 image is required");
        }

        if (files.size() > 5) {
            throw new BadRequestException("Maximum 5 images allowed");
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new BadRequestException("Empty file is not allowed");
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                throw new BadRequestException("File size must not exceed 5MB: " + file.getOriginalFilename());
            }

            if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
                throw new BadRequestException("Invalid file type. Only JPEG, PNG, WEBP are allowed: " + file.getOriginalFilename());
            }
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String extractKeyFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BadRequestException("Invalid S3 URL/key: " + imageUrl);
        }

        if (!imageUrl.startsWith(s3Properties.getEndpoint())) {
            return imageUrl;
        }

        String prefix = s3Properties.getEndpoint() + "/" + s3Properties.getBucketName() + "/";
        if (!imageUrl.startsWith(prefix)) {
            throw new BadRequestException("Invalid Garage URL: " + imageUrl);
        }

        return imageUrl.substring(prefix.length());
    }
}
