package com.api.v1.inventory.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockOperationRequest {

    @NotNull(message = "Product ID is required")
    @Schema(description = "ID of the target product")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be greater than zero")
    @Schema(description = "Quantity of items to add or deduct")
    private Integer quantity;

    @Size(max = 255, message = "Note cannot exceed 255 characters")
    @Schema(description = "Optional transaction note or reference details")
    private String note;
}
