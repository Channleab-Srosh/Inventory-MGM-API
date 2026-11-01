package com.api.v1.inventory.model.response;

import com.api.v1.inventory.common.response.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated inventory report response including overall inventory totals")
public class InventoryReportPageResponse extends PageResponse<InventoryReportResponse> {

    @Schema(description = "Total number of unique items/products matching the filter", example = "29")
    private long totalProducts;

    @Schema(description = "Total units / physical quantity across all matching products", example = "2845")
    private long totalUnits;

    @Schema(description = "Total inventory valuation (sum of stockQuantity * unitPrice)", example = "73940.50")
    private BigDecimal totalInventoryValuation;

    @Schema(description = "Total items currently in stock (stockQuantity > minimumStock)", example = "22")
    private long inStockCount;

    @Schema(description = "Total items low in stock (0 < stockQuantity <= minimumStock)", example = "5")
    private long lowStockCount;

    @Schema(description = "Total items out of stock (stockQuantity <= 0)", example = "2")
    private long outOfStockCount;

    @Builder(builderMethodName = "inventoryPageBuilder")
    public InventoryReportPageResponse(
            List<InventoryReportResponse> content,
            int pageNumber,
            int pageSize,
            long totalElements,
            int totalPages,
            boolean last,
            long totalProducts,
            long totalUnits,
            BigDecimal totalInventoryValuation,
            long inStockCount,
            long lowStockCount,
            long outOfStockCount
    ) {
        super(content, pageNumber, pageSize, totalElements, totalPages, last);
        this.totalProducts = totalProducts;
        this.totalUnits = totalUnits;
        this.totalInventoryValuation = totalInventoryValuation;
        this.inStockCount = inStockCount;
        this.lowStockCount = lowStockCount;
        this.outOfStockCount = outOfStockCount;
    }
}
