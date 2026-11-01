package com.api.v1.inventory.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Aggregated stock summary and category breakdown report based on applied filters")
public class InventorySummaryReportResponse {

    @Schema(description = "High-level stock summary metrics")
    private StockSummaryReport stockSummary;

    @Schema(description = "Category-level stock breakdown")
    @Builder.Default
    private List<CategoryStockBreakdown> categoryBreakdown = new ArrayList<>();

    // Direct convenience properties for UI components
    @Schema(description = "Total number of unique items/products matching the filter", example = "42")
    private long totalItems;

    @Schema(description = "Total units across all matching products", example = "1250")
    private long totalUnits;

    @Schema(description = "Total inventory valuation (sum of stockQuantity * unitPrice)", example = "15420.50")
    private BigDecimal totalInventoryValuation;

    @Schema(description = "Total items currently in stock (stockQuantity > minimumStock)", example = "30")
    private long inStockCount;

    @Schema(description = "Total items low in stock (0 < stockQuantity <= minimumStock)", example = "8")
    private long lowStockCount;

    @Schema(description = "Total items out of stock (stockQuantity <= 0)", example = "4")
    private long outOfStockCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Stock summary report metrics")
    public static class StockSummaryReport {
        @Schema(description = "Total number of unique items/products", example = "42")
        private long totalItems;

        @Schema(description = "Total units across all products", example = "1250")
        private long totalUnits;

        @Schema(description = "Total valuation of inventory", example = "15420.50")
        private BigDecimal totalInventoryValuation;

        @Schema(description = "Breakdown by stock health status")
        private StockStatusCount status;

        @Schema(description = "In-stock items count", example = "30")
        private long inStockCount;

        @Schema(description = "Low-stock items count", example = "8")
        private long lowStockCount;

        @Schema(description = "Out-of-stock items count", example = "4")
        private long outOfStockCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Product count grouped by stock health status")
    public static class StockStatusCount {
        @Schema(description = "Number of in-stock items", example = "30")
        private long inStock;

        @Schema(description = "Number of low-stock items", example = "8")
        private long lowStock;

        @Schema(description = "Number of out-of-stock items", example = "4")
        private long outOfStock;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Category-level stock breakdown item")
    public static class CategoryStockBreakdown {
        @Schema(description = "Category ID (null for uncategorized)", example = "1")
        private Long categoryId;

        @Schema(description = "Category name", example = "Beverages")
        private String categoryName;

        @Schema(description = "Total distinct products/items in this category", example = "12")
        private long totalItems;

        @Schema(description = "Total stock units in this category", example = "350")
        private long totalUnits;

        @Schema(description = "Total valuation of inventory in this category", example = "4200.00")
        private BigDecimal totalValuation;
    }
}
