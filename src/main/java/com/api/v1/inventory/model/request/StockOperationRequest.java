package com.api.v1.inventory.model.request;

import com.api.v1.inventory.model.enumerate.StockOutReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class StockOperationRequest {

    @NotNull(message = "Product ID is required")
    @Schema(description = "ID of the target product")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be greater than zero")
    @Schema(description = "Quantity of items to add or deduct")
    private Integer quantity;

    @DecimalMin(value = "0.0", inclusive = true, message = "Unit cost must be non-negative")
    @Schema(description = "Optional unit cost per item for stock-in operations")
    private BigDecimal unitCost;

    @Size(max = 255, message = "Note cannot exceed 255 characters")
    @Schema(description = "Optional transaction note or reference details")
    private String note;

    @Schema(description = "Reason for stock-out (SALE, DAMAGE, OTHER). Required when performing a stock-out operation.")
    private StockOutReason reason;

    @Schema(description = "ID of the supplier. Required for Stock In operations.")
    private Long supplierId;

    @Schema(description = "ID of the customer. Required for Stock Out operations with reason SALE. Not required if reason is DAMAGE or OTHER.")
    private Long customerId;

    @Schema(description = "Optional transaction timestamp (defaults to current time if omitted)")
    private java.time.Instant transactionDate;
}
