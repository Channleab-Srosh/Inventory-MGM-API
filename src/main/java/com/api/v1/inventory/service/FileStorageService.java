package com.api.v1.inventory.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Stores the uploaded image file and returns the public URL.
     *
     * @param file the multipart image file
     * @return the public URL that can be used to access the stored image
     */
    String storeProductImage(MultipartFile file);

    /**
     * Deletes the image identified by the given public URL (best-effort).
     *
     * @param imageUrl the public URL previously returned by {@link #storeProductImage}
     */
    void deleteByUrl(String imageUrl);
}
