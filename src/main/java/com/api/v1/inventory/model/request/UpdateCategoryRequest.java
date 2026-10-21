package com.api.v1.inventory.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must not exceed 100 characters")
    @Schema(description = "Updated category name", example = "Electronics & Accessories")
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Schema(description = "Updated category description", example = "Updated electronic devices, computer hardware, and accessories")
    private String description;
}
