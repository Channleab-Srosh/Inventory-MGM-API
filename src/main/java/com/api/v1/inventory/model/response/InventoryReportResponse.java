package com.api.v1.inventory.model.response;

import com.api.v1.inventory.model.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReportResponse {

    private Long id;
    private String sku;
    private String name;
    private String categoryName;
    private String supplierName;
    private BigDecimal unitPrice;
    private Integer stockQuantity;
    private Integer minimumStock;
    private BigDecimal totalValue;
    private boolean lowStock;
    private boolean active;
    private Instant updatedAt;

    public static InventoryReportResponse fromEntity(Product product) {
        if (product == null) {
            return null;
        }

        BigDecimal unitPrice = product.getUnitPrice() != null ? product.getUnitPrice() : BigDecimal.ZERO;
        int qty = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        BigDecimal totalValue = unitPrice.multiply(BigDecimal.valueOf(qty));

        boolean isLow = product.getStockQuantity() != null &&
                product.getMinimumStock() != null &&
                product.getStockQuantity() <= product.getMinimumStock();

        return InventoryReportResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .supplierName(product.getSupplier() != null ? product.getSupplier().getName() : null)
                .unitPrice(unitPrice)
                .stockQuantity(qty)
                .minimumStock(product.getMinimumStock() != null ? product.getMinimumStock() : 0)
                .totalValue(totalValue)
                .lowStock(isLow)
                .active(product.isActive())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
