package fr.minhnn.touristapi.destination;

import java.util.List;

public interface S3Service {
    /**
     * Upload multiple images to S3
     * @param files List of image files
     * @param folder Folder path in S3 (e.g., "destinations/123")
     * @return List of S3 URLs
     */
    List<String> uploadImages(List<Destination.ImageFile> files, String folder);

    /**
     * Delete an image from S3
     * @param imageUrl Full S3 URL
     */
    void deleteImage(String imageUrl);

    /**
     * Delete multiple images from S3
     * @param imageUrls List of S3 URLs
     */
    void deleteImages(List<String> imageUrls);
}
