package com.api.v1.inventory.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    private long totalProducts;
    private long totalCategories;
    private long totalSuppliers;
    private long totalStockQuantity;
    private BigDecimal totalInventoryValue;
    private long lowStockCount;
    private long outOfStockCount;
    private long totalStockInQuantity;
    private long totalStockOutQuantity;
}
