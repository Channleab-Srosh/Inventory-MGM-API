package com.api.v1.inventory.service.impl;

import com.api.v1.inventory.common.exception.BadRequestException;
import com.api.v1.inventory.config.AppProperty;
import com.api.v1.inventory.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024L; // 5 MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );

    private final AppProperty appProperty;
    private Path uploadDir;

    @PostConstruct
    public void init() {
        uploadDir = Paths.get(appProperty.getUpload().getDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDir);
            log.info("Upload directory initialized at: {}", uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory: " + uploadDir, e);
        }
    }

    @Override
    public String storeProductImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file must not be empty");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("Image file size must not exceed 5 MB");
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "Unsupported image type '" + contentType + "'. Allowed: JPEG, PNG, GIF, WEBP");
        }

        // Extract and validate extension from original filename
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BadRequestException(
                    "Unsupported file extension '" + extension + "'. Allowed: .jpg, .jpeg, .png, .gif, .webp");
        }

        // Generate a unique filename to avoid collisions and path traversal
        String storedFilename = UUID.randomUUID() + extension.toLowerCase();
        Path targetPath = uploadDir.resolve(storedFilename);

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored product image: {}", storedFilename);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store image file: " + storedFilename, e);
        }

        String baseUrl = appProperty.getUpload().getBaseUrl();
        return baseUrl.endsWith("/") ? baseUrl + storedFilename : baseUrl + "/" + storedFilename;
    }

    @Override
    public void deleteByUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        try {
            String baseUrl = appProperty.getUpload().getBaseUrl();
            String prefix = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
            if (!imageUrl.startsWith(prefix)) {
                // External URL — not managed by us
                return;
            }
            String filename = imageUrl.substring(prefix.length());
            // Prevent path traversal
            Path filePath = uploadDir.resolve(filename).normalize();
            if (!filePath.startsWith(uploadDir)) {
                log.warn("Attempted path traversal in image deletion: {}", imageUrl);
                return;
            }
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Deleted old product image: {}", filename);
            }
        } catch (IOException e) {
            log.warn("Could not delete image file for URL '{}': {}", imageUrl, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank() || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
