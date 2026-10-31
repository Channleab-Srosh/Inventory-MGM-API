package com.api.v1.inventory.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {

    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must not exceed 50 characters")
    @Schema(description = "Unique Stock Keeping Unit code")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name must not exceed 150 characters")
    @Schema(description = "Product display name")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    @Schema(description = "Detailed product description")
    private String description;

    @Size(max = 2048, message = "Image URL must not exceed 2048 characters")
    @Schema(description = "URL of the product image")
    private String imageUrl;

    @NotNull(message = "Category ID is required")
    @Schema(description = "ID of associated category")
    private Long categoryId;

    @Schema(description = "ID of associated supplier")
    private Long supplierId;

    @Builder.Default
    @Schema(description = "Unit of measure")
    private com.api.v1.inventory.model.enumerate.UnitOfMeasure unitOfMeasure = com.api.v1.inventory.model.enumerate.UnitOfMeasure.PCS;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.00", message = "Unit price cannot be negative")
    @Schema(description = "Selling price per unit")
    private BigDecimal unitPrice;

    @NotNull(message = "Minimum stock is required")
    @Min(value = 0, message = "Minimum stock cannot be negative")
    @Schema(description = "Threshold quantity for low stock alerts")
    private Integer minimumStock;

    @NotNull(message = "Active status is required")
    @Schema(description = "Active status of the product")
    private Boolean active;
}
