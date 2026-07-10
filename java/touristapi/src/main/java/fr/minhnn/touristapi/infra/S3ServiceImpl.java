package fr.minhnn.touristapi.infra;

import fr.minhnn.touristapi.config.S3Properties;
import fr.minhnn.touristapi.destination.Destination;
import fr.minhnn.touristapi.destination.S3Service;
import fr.minhnn.touristapi.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
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
@RequiredArgsConstructor
@Slf4j
public class S3ServiceImpl implements S3Service {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofMinutes(15);
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L; // 5MB

    @Override
    public List<String> uploadImages(List<Destination.ImageFile> files, String folder) {
        List<MultipartFile> multipartFiles = new ArrayList<>();
        files.parallelStream()
                .forEach(file -> {
                    log.debug("Preparing to upload image: {}", file.fileName());
                    synchronized (multipartFiles) {
                        multipartFiles.add(MultipartFileAdapter.toMultipartFile(file));
                    }
                });

        validateImages(multipartFiles);

        List<String> uploadedUrls = new ArrayList<>();
        multipartFiles.parallelStream()
                .forEach(multipartFile -> {
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

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            deleteImages(uploadedUrls);
                        }
                    }
                }
        );

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

        String url = String.format("https://%s.s3.%s.amazonaws.com/%s",
                s3Properties.getBucketName(),
                Region.of(s3Properties.getRegion()),
                key);

        log.info("Uploaded image to S3: {}", url);
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
            log.info("Deleted image from S3: {}", imageUrl);
        } catch (Exception e) {
            log.error("Failed to delete image from S3: {}", imageUrl, e);
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
            log.error("No images provided for upload");
            throw new BadRequestException("At least 1 image is required");
        }

        if (files.size() > 5) {
            log.error("Too many images provided: {}. Maximum allowed is 5", files.size());
            throw new BadRequestException("Maximum 5 images allowed");
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                log.error("Empty file provided: {}", file.getOriginalFilename());
                throw new BadRequestException("Empty file is not allowed");
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                log.error("File size exceeds limit: {} ({} bytes)", file.getOriginalFilename(), file.getSize());
                throw new BadRequestException("File size must not exceed 5MB: " + file.getOriginalFilename());
            }

            String contentType = file.getContentType();
            if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
                log.error("Invalid file type: {} ({}). Allowed types are: {}", file.getOriginalFilename(), contentType, ALLOWED_CONTENT_TYPES);
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
            log.error("Invalid S3 URL/key format: {}", imageUrl);
            throw new BadRequestException("Invalid S3 URL/key: " + imageUrl);
        }

        if (!imageUrl.contains("amazonaws.com/")) {
            return imageUrl;
        }

        // Extract key from URL: https://bucket.s3.region.amazonaws.com/folder/file.jpg -> folder/file.jpg
        String[] parts = imageUrl.split("\\.amazonaws\\.com/", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            log.error("Invalid S3 URL format: {}", imageUrl);
            throw new BadRequestException("Invalid S3 URL: " + imageUrl);
        }
        return parts[1];
    }
}
