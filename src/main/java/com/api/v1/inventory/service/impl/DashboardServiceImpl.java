package com.api.v1.inventory.service.impl;

import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.entity.Product;
import com.api.v1.inventory.model.enumerate.TransactionType;
import com.api.v1.inventory.model.response.DashboardSummaryResponse;
import com.api.v1.inventory.model.response.ProductResponse;
import com.api.v1.inventory.repository.CategoryRepository;
import com.api.v1.inventory.repository.ProductRepository;
import com.api.v1.inventory.repository.StockTransactionRepository;
import com.api.v1.inventory.repository.SupplierRepository;
import com.api.v1.inventory.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final StockTransactionRepository stockTransactionRepository;

    @Override
    public DashboardSummaryResponse getDashboardSummary() {
        long totalProducts = productRepository.countByActiveTrue();
        long totalCategories = categoryRepository.count();
        long totalSuppliers = supplierRepository.count();
        long totalStockQuantity = productRepository.sumTotalStockQuantity();
        BigDecimal totalInventoryValue = productRepository.sumTotalInventoryValue();
        long lowStockCount = productRepository.countLowStockProducts();
        long outOfStockCount = productRepository.countOutOfStockProducts();
        long totalStockIn = stockTransactionRepository.sumQuantityByType(TransactionType.STOCK_IN);
        long totalStockOut = stockTransactionRepository.sumQuantityByType(TransactionType.STOCK_OUT);

        log.debug("Dashboard summary computed: products={}, stockQty={}, value={}",
                totalProducts, totalStockQuantity, totalInventoryValue);

        return DashboardSummaryResponse.builder()
                .totalProducts(totalProducts)
                .totalCategories(totalCategories)
                .totalSuppliers(totalSuppliers)
                .totalStockQuantity(totalStockQuantity)
                .totalInventoryValue(totalInventoryValue)
                .lowStockCount(lowStockCount)
                .outOfStockCount(outOfStockCount)
                .totalStockInQuantity(totalStockIn)
                .totalStockOutQuantity(totalStockOut)
                .build();
    }

    @Override
    public PageResponse<ProductResponse> getLowStockProducts(Pageable pageable) {
        Page<Product> productPage = productRepository.findLowStockProducts(pageable);
        return PageResponse.from(productPage.map(ProductResponse::fromEntity));
    }
}
