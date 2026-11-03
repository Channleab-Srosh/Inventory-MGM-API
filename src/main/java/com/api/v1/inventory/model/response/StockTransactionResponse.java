package com.api.v1.inventory.model.response;

import com.api.v1.inventory.model.entity.StockTransaction;
import com.api.v1.inventory.model.enumerate.StockOutReason;
import com.api.v1.inventory.model.enumerate.TransactionType;
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
public class StockTransactionResponse {

    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private Long supplierId;
    private String supplierName;
    private Long customerId;
    private String customerName;
    private TransactionType transactionType;
    private StockOutReason reason;
    private com.api.v1.inventory.model.enumerate.UnitOfMeasure unitOfMeasure;
    private Integer quantity;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private BigDecimal unitCost;
    private String note;
    private Long createdById;
    private String createdByUsername;
    private Instant createdAt;

    public static StockTransactionResponse fromEntity(StockTransaction tx) {
        if (tx == null) {
            return null;
        }
        return StockTransactionResponse.builder()
                .id(tx.getId())
                .productId(tx.getProduct() != null ? tx.getProduct().getId() : null)
                .productSku(tx.getProduct() != null ? tx.getProduct().getSku() : null)
                .productName(tx.getProduct() != null ? tx.getProduct().getName() : null)
                .supplierId(tx.getSupplier() != null ? tx.getSupplier().getId() : null)
                .supplierName(tx.getSupplier() != null ? tx.getSupplier().getName() : null)
                .customerId(tx.getCustomer() != null ? tx.getCustomer().getId() : null)
                .customerName(tx.getCustomer() != null ? tx.getCustomer().getName() : null)
                .transactionType(tx.getTransactionType())
                .reason(tx.getReason())
                .unitOfMeasure(tx.getProduct() != null ? tx.getProduct().getUnitOfMeasure() : null)
                .quantity(tx.getQuantity())
                .quantityBefore(tx.getQuantityBefore())
                .quantityAfter(tx.getQuantityAfter())
                .unitCost(tx.getUnitCost())
                .note(tx.getNote())
                .createdById(tx.getCreatedBy() != null ? tx.getCreatedBy().getId() : null)
                .createdByUsername(tx.getCreatedBy() != null ? tx.getCreatedBy().getUsername() : null)
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
