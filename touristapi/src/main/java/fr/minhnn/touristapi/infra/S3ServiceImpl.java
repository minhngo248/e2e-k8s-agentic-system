package fr.minhnn.touristapi.infra;

import fr.minhnn.touristapi.destination.Destination;
import fr.minhnn.touristapi.destination.S3Service;
import fr.minhnn.touristapi.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class S3ServiceImpl implements S3Service {
    private final S3Client s3Client;

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L; // 5MB
    private static final String BUCKET_NAME = "tourist-images";
    private static final String REGION = "eu-west-3";

    @Override
    public List<String> uploadImages(List<Destination.ImageFile> files, String folder) {
        List<MultipartFile> multipartFiles = new ArrayList<>();
        files.parallelStream()
                .forEach(file -> {
                    log.info("Preparing to upload image: {}", file.fileName());
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
                .bucket(BUCKET_NAME)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        String url = String.format("https://%s.s3.%s.amazonaws.com/%s",
                BUCKET_NAME,
                REGION,
                key);

        log.info("Uploaded image to S3: {}", url);
        return url;
    }

    @Override
    public void deleteImage(String imageUrl) {
        try {
            String key = extractKeyFromUrl(imageUrl);
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(BUCKET_NAME)
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

            String contentType = file.getContentType();
            if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
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
        // Extract key from URL: https://bucket.s3.region.amazonaws.com/folder/file.jpg -> folder/file.jpg
        String[] parts = imageUrl.split(".amazonaws.com/");
        if (parts.length < 2) {
            throw new BadRequestException("Invalid S3 URL: " + imageUrl);
        }
        return parts[1];
    }
}
