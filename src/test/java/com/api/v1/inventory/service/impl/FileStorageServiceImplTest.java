package com.api.v1.inventory.service.impl;

import com.api.v1.inventory.config.AppProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceImplTest {

    private FileStorageServiceImpl fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        AppProperty appProperty = new AppProperty();
        appProperty.getUpload().setDir(tempDir.toString());
        appProperty.getUpload().setBaseUrl("http://localhost:8080/uploads/images");

        fileStorageService = new FileStorageServiceImpl(appProperty);
        fileStorageService.init();
    }

    @Test
    @DisplayName("Should successfully store .jfif image with image/jpeg content type")
    void shouldStoreJfifImageWithJpegContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample_product.jfif",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}
        );

        String url = fileStorageService.storeProductImage(file);
        assertNotNull(url);
        assertTrue(url.endsWith(".jfif"));
        assertTrue(url.startsWith("http://localhost:8080/uploads/images/"));
    }

    @Test
    @DisplayName("Should successfully store .jfif image with image/jfif content type")
    void shouldStoreJfifImageWithJfifContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.JFIF",
                "image/jfif",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}
        );

        String url = fileStorageService.storeProductImage(file);
        assertNotNull(url);
        assertTrue(url.endsWith(".jfif"));
    }
}
