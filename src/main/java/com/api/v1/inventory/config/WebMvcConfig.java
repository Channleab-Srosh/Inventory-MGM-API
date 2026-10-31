package com.api.v1.inventory.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Exposes uploaded product images as publicly accessible static resources
 * at the URL path {@code /uploads/images/**}.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AppProperty appProperty;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadDir = Paths.get(appProperty.getUpload().getDir())
                .toAbsolutePath().normalize().toUri().toString();

        // Ensure trailing slash for resource location
        if (!uploadDir.endsWith("/")) {
            uploadDir = uploadDir + "/";
        }

        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations(uploadDir);
    }
}
