package io.psyllabus.platform.common.service;

import java.io.IOException;

/**
 * Interface for storage operations (Cloudinary, S3, etc.)
 */
public interface StorageService {

    /**
     * Upload an image and return its public URL.
     * 
     * @param bytes the image data
     * @param folder the target folder (e.g. "school-emblems", "notice-attachments")
     * @return the secure public URL
     */
    String uploadImage(byte[] bytes, String folder) throws IOException;

    /**
     * Delete an image by its public ID or URL.
     */
    void deleteImage(String publicId) throws IOException;
}
