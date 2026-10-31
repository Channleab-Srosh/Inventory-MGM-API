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
public class ProductResponse {

    private Long id;
    private String sku;
    private String name;
    private String description;
    private String imageUrl;
    private CategoryResponse category;
    private SupplierResponse supplier;
    private com.api.v1.inventory.model.enumerate.UnitOfMeasure unitOfMeasure;
    private BigDecimal unitPrice;
    private Integer stockQuantity;
    private Integer minimumStock;
    private boolean active;
    private boolean lowStock;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProductResponse fromEntity(Product product) {
        if (product == null) {
            return null;
        }

        boolean isLow = product.getStockQuantity() != null &&
                product.getMinimumStock() != null &&
                product.getStockQuantity() <= product.getMinimumStock();

        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .category(CategoryResponse.fromEntity(product.getCategory()))
                .supplier(SupplierResponse.fromEntity(product.getSupplier()))
                .unitOfMeasure(product.getUnitOfMeasure())
                .unitPrice(product.getUnitPrice())
                .stockQuantity(product.getStockQuantity())
                .minimumStock(product.getMinimumStock())
                .active(product.isActive())
                .lowStock(isLow)
                .version(product.getVersion())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
