package com.api.v1.inventory.service.impl;

import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.common.util.ReportDateRange;
import com.api.v1.inventory.model.entity.Product;
import com.api.v1.inventory.model.enumerate.ReportPeriod;
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
import java.time.Instant;

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
        return getDashboardSummary(null, null, null, null);
    }

    @Override
    public DashboardSummaryResponse getDashboardSummary(ReportPeriod period, Instant fromDate, Instant toDate, String zoneId) {
        if (period == null && fromDate == null && toDate == null) {
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

        ReportDateRange range = ReportDateRange.resolve(period, fromDate, toDate, zoneId);
        long totalProducts = productRepository.countByActiveTrue();
        long totalCategories = categoryRepository.count();
        long totalSuppliers = supplierRepository.count();

        long totalStockIn = stockTransactionRepository.sumQuantityByTypeAndDateRange(
                TransactionType.STOCK_IN, range.getFromDate(), range.getToDate()
        );
        long totalStockOut = stockTransactionRepository.sumQuantityByTypeAndDateRange(
                TransactionType.STOCK_OUT, range.getFromDate(), range.getToDate()
        );
        long netPeriodStock = totalStockIn - totalStockOut;

        BigDecimal stockInValue = stockTransactionRepository.sumValueByTypeAndDateRange(
                TransactionType.STOCK_IN, range.getFromDate(), range.getToDate()
        );
        BigDecimal stockOutValue = stockTransactionRepository.sumValueByTypeAndDateRange(
                TransactionType.STOCK_OUT, range.getFromDate(), range.getToDate()
        );
        BigDecimal netPeriodValue = stockInValue.subtract(stockOutValue);

        long lowStockCount = productRepository.countLowStockProducts();
        long outOfStockCount = productRepository.countOutOfStockProducts();

        log.debug("Dashboard summary computed (period={}): stockIn={}, stockOut={}, netQty={}, netVal={}",
                period, totalStockIn, totalStockOut, netPeriodStock, netPeriodValue);

        return DashboardSummaryResponse.builder()
                .totalProducts(totalProducts)
                .totalCategories(totalCategories)
                .totalSuppliers(totalSuppliers)
                .totalStockQuantity(netPeriodStock)
                .totalInventoryValue(netPeriodValue)
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
