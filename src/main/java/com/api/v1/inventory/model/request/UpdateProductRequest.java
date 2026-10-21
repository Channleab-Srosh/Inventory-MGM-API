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
    @Schema(description = "Unique Stock Keeping Unit code", example = "PRD-LOGI-MX3S")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name must not exceed 150 characters")
    @Schema(description = "Product display name", example = "Logitech MX Master 3S Wireless Mouse (Graphite)")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    @Schema(description = "Detailed product description", example = "Updated high-precision wireless mouse with Quiet Click switches")
    private String description;

    @NotNull(message = "Category ID is required")
    @Schema(description = "ID of associated category", example = "1")
    private Long categoryId;

    @Schema(description = "ID of associated supplier", example = "1")
    private Long supplierId;

    @Builder.Default
    @Schema(description = "Unit of measure", example = "PCS")
    private com.api.v1.inventory.model.enumerate.UnitOfMeasure unitOfMeasure = com.api.v1.inventory.model.enumerate.UnitOfMeasure.PCS;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.00", message = "Unit price cannot be negative")
    @Schema(description = "Selling price per unit", example = "89.99")
    private BigDecimal unitPrice;

    @NotNull(message = "Minimum stock is required")
    @Min(value = 0, message = "Minimum stock cannot be negative")
    @Schema(description = "Threshold quantity for low stock alerts", example = "15")
    private Integer minimumStock;

    @NotNull(message = "Active status is required")
    @Schema(description = "Active status of the product", example = "true")
    private Boolean active;
}
