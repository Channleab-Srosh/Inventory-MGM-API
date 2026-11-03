package com.api.v1.inventory.service.impl;

import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.common.util.ReportDateRange;
import com.api.v1.inventory.model.entity.Product;
import com.api.v1.inventory.model.entity.StockTransaction;
import com.api.v1.inventory.model.enumerate.ExportFormat;
import com.api.v1.inventory.model.enumerate.ReportPeriod;
import com.api.v1.inventory.model.enumerate.StockOutReason;
import com.api.v1.inventory.model.enumerate.StockStatusFilter;
import com.api.v1.inventory.model.enumerate.TransactionType;
import com.api.v1.inventory.model.response.ExportedFile;
import com.api.v1.inventory.model.response.InventoryReportPageResponse;
import com.api.v1.inventory.model.response.InventoryReportResponse;
import com.api.v1.inventory.model.response.InventorySummaryReportResponse;
import com.api.v1.inventory.model.response.StockTransactionResponse;
import com.api.v1.inventory.repository.ProductRepository;
import com.api.v1.inventory.repository.StockTransactionRepository;
import com.api.v1.inventory.service.ReportService;
import com.api.v1.inventory.service.StockService;
import com.api.v1.inventory.service.report.ExcelReportService;
import com.api.v1.inventory.service.report.PdfReportService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final StockService stockService;
    private final StockTransactionRepository stockTransactionRepository;
    private final ProductRepository productRepository;
    private final ExcelReportService excelReportService;
    private final PdfReportService pdfReportService;

    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final DateTimeFormatter FILENAME_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    public PageResponse<StockTransactionResponse> getStockTransactionReport(
            Long productId,
            TransactionType type,
            StockOutReason reason,
            Long userId,
            ReportPeriod period,
            Instant fromDate,
            Instant toDate,
            String zoneId,
            Pageable pageable
    ) {
        ReportDateRange range = ReportDateRange.resolve(period, fromDate, toDate, zoneId);

        log.debug("Generating stock transaction report: productId={}, type={}, reason={}, userId={}, period={}, fromDate={}, toDate={}",
                productId, type, reason, userId, period, range.getFromDate(), range.getToDate());

        return stockService.getTransactionHistory(productId, type, reason, userId, range.getFromDate(), range.getToDate(), pageable);
    }

    @Override
    public InventoryReportPageResponse getInventoryReport(
            String keyword,
            Long categoryId,
            Long supplierId,
            Boolean active,
            StockStatusFilter stockStatus,
            Boolean lowStockOnly,
            ReportPeriod period,
            Instant fromDate,
            Instant toDate,
            String zoneId,
            Pageable pageable
    ) {
        ReportDateRange range = ReportDateRange.resolve(period, fromDate, toDate, zoneId);

        String searchKeyword = (keyword != null && !keyword.trim().isEmpty())
                ? "%" + keyword.trim().toLowerCase() + "%"
                : null;

        String statusStr = (stockStatus != null) ? stockStatus.name() : StockStatusFilter.ALL.name();

        log.debug("Generating inventory report: keyword={}, categoryId={}, supplierId={}, active={}, stockStatus={}, lowStockOnly={}, fromDate={}, toDate={}",
                keyword, categoryId, supplierId, active, statusStr, lowStockOnly, range.getFromDate(), range.getToDate());

        Page<Product> page = productRepository.searchInventoryReport(
                searchKeyword,
                categoryId,
                supplierId,
                active,
                statusStr,
                lowStockOnly,
                range.getFromDate(),
                range.getToDate(),
                pageable
        );

        List<Product> allProducts = productRepository.findInventoryReportList(
                searchKeyword,
                categoryId,
                supplierId,
                active,
                statusStr,
                lowStockOnly,
                range.getFromDate(),
                range.getToDate()
        );

        long totalProducts = (allProducts != null) ? allProducts.size() : page.getTotalElements();
        long totalUnits = 0L;
        BigDecimal totalValuation = BigDecimal.ZERO;
        long inStockCount = 0L;
        long lowStockCount = 0L;
        long outOfStockCount = 0L;

        if (allProducts != null) {
            for (Product p : allProducts) {
                int qty = (p.getStockQuantity() != null) ? p.getStockQuantity() : 0;
                int min = (p.getMinimumStock() != null) ? p.getMinimumStock() : 0;
                BigDecimal price = (p.getUnitPrice() != null) ? p.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal itemValuation = price.multiply(BigDecimal.valueOf(qty));

                totalUnits += qty;
                totalValuation = totalValuation.add(itemValuation);

                if (qty <= 0) {
                    outOfStockCount++;
                } else if (qty <= min) {
                    lowStockCount++;
                } else {
                    inStockCount++;
                }
            }
        }

        Page<InventoryReportResponse> mappedPage = page.map(InventoryReportResponse::fromEntity);

        return InventoryReportPageResponse.inventoryPageBuilder()
                .content(mappedPage.getContent())
                .pageNumber(mappedPage.getNumber())
                .pageSize(mappedPage.getSize())
                .totalElements(mappedPage.getTotalElements())
                .totalPages(mappedPage.getTotalPages())
                .last(mappedPage.isLast())
                .totalProducts(totalProducts)
                .totalUnits(totalUnits)
                .totalInventoryValuation(totalValuation)
                .inStockCount(inStockCount)
                .lowStockCount(lowStockCount)
                .outOfStockCount(outOfStockCount)
                .build();
    }

    @Override
    public InventorySummaryReportResponse getInventorySummaryReport(
            String keyword,
            Long categoryId,
            Long supplierId,
            Boolean active,
            StockStatusFilter stockStatus,
            Boolean lowStockOnly,
            ReportPeriod period,
            Instant fromDate,
            Instant toDate,
            String zoneId
    ) {
        ReportDateRange range = ReportDateRange.resolve(period, fromDate, toDate, zoneId);

        String searchKeyword = (keyword != null && !keyword.trim().isEmpty())
                ? "%" + keyword.trim().toLowerCase() + "%"
                : null;
        String statusStr = (stockStatus != null) ? stockStatus.name() : StockStatusFilter.ALL.name();

        List<Product> products = productRepository.findInventoryReportList(
                searchKeyword,
                categoryId,
                supplierId,
                active,
                statusStr,
                lowStockOnly,
                range.getFromDate(),
                range.getToDate()
        );

        long totalItems = products.size();
        long totalUnits = 0L;
        BigDecimal totalValuation = BigDecimal.ZERO;
        long inStockCount = 0L;
        long lowStockCount = 0L;
        long outOfStockCount = 0L;

        Map<Long, CategoryAccumulator> categoryMap = new LinkedHashMap<>();

        for (Product p : products) {
            int qty = (p.getStockQuantity() != null) ? p.getStockQuantity() : 0;
            int min = (p.getMinimumStock() != null) ? p.getMinimumStock() : 0;
            BigDecimal price = (p.getUnitPrice() != null) ? p.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal itemValuation = price.multiply(BigDecimal.valueOf(qty));

            totalUnits += qty;
            totalValuation = totalValuation.add(itemValuation);

            if (qty <= 0) {
                outOfStockCount++;
            } else if (qty <= min) {
                lowStockCount++;
            } else {
                inStockCount++;
            }

            Long catId = p.getCategory() != null ? p.getCategory().getId() : null;
            String catName = p.getCategory() != null ? p.getCategory().getName() : "Uncategorized";

            categoryMap.computeIfAbsent(catId, k -> new CategoryAccumulator(catId, catName))
                    .addItem(qty, itemValuation);
        }

        // When a specific period filter is active, totalUnits and valuation reflect the net movement for the period
        if (range.getFromDate() != null || range.getToDate() != null) {
            long totalStockIn = stockTransactionRepository.sumQuantityByTypeAndDateRange(
                    TransactionType.STOCK_IN, range.getFromDate(), range.getToDate()
            );
            long totalStockOut = stockTransactionRepository.sumQuantityByTypeAndDateRange(
                    TransactionType.STOCK_OUT, range.getFromDate(), range.getToDate()
            );
            totalUnits = totalStockIn - totalStockOut;

            BigDecimal stockInVal = stockTransactionRepository.sumValueByTypeAndDateRange(
                    TransactionType.STOCK_IN, range.getFromDate(), range.getToDate()
            );
            BigDecimal stockOutVal = stockTransactionRepository.sumValueByTypeAndDateRange(
                    TransactionType.STOCK_OUT, range.getFromDate(), range.getToDate()
            );
            totalValuation = stockInVal.subtract(stockOutVal);

            inStockCount = stockTransactionRepository.countDistinctProductsByTypeAndDateRange(
                    TransactionType.STOCK_IN, range.getFromDate(), range.getToDate()
            );
        }

        List<InventorySummaryReportResponse.CategoryStockBreakdown> categoryBreakdowns = categoryMap.values().stream()
                .sorted(Comparator.comparing(CategoryAccumulator::getCategoryName, String.CASE_INSENSITIVE_ORDER))
                .map(acc -> InventorySummaryReportResponse.CategoryStockBreakdown.builder()
                        .categoryId(acc.getCategoryId())
                        .categoryName(acc.getCategoryName())
                        .totalItems(acc.getTotalItems())
                        .totalUnits(acc.getTotalUnits())
                        .totalValuation(acc.getTotalValuation())
                        .build())
                .toList();

        InventorySummaryReportResponse.StockStatusCount statusCount = InventorySummaryReportResponse.StockStatusCount.builder()
                .inStock(inStockCount)
                .lowStock(lowStockCount)
                .outOfStock(outOfStockCount)
                .build();

        InventorySummaryReportResponse.StockSummaryReport stockSummary = InventorySummaryReportResponse.StockSummaryReport.builder()
                .totalItems(totalItems)
                .totalUnits(totalUnits)
                .totalInventoryValuation(totalValuation)
                .status(statusCount)
                .inStockCount(inStockCount)
                .lowStockCount(lowStockCount)
                .outOfStockCount(outOfStockCount)
                .build();

        return InventorySummaryReportResponse.builder()
                .stockSummary(stockSummary)
                .categoryBreakdown(categoryBreakdowns)
                .totalItems(totalItems)
                .totalUnits(totalUnits)
                .totalInventoryValuation(totalValuation)
                .inStockCount(inStockCount)
                .lowStockCount(lowStockCount)
                .outOfStockCount(outOfStockCount)
                .build();
    }

    @Override
    public ExportedFile exportInventoryReport(
            ExportFormat format,
            String keyword,
            Long categoryId,
            Long supplierId,
            Boolean active,
            StockStatusFilter stockStatus,
            Boolean lowStockOnly,
            ReportPeriod period,
            Instant fromDate,
            Instant toDate,
            String zoneId
    ) {
        ReportDateRange range = ReportDateRange.resolve(period, fromDate, toDate, zoneId);

        String searchKeyword = (keyword != null && !keyword.trim().isEmpty())
                ? "%" + keyword.trim().toLowerCase() + "%"
                : null;
        String statusStr = (stockStatus != null) ? stockStatus.name() : StockStatusFilter.ALL.name();

        List<Product> products = productRepository.findInventoryReportList(
                searchKeyword,
                categoryId,
                supplierId,
                active,
                statusStr,
                lowStockOnly,
                range.getFromDate(),
                range.getToDate()
        );

        List<InventoryReportResponse> items = products.stream()
                .map(InventoryReportResponse::fromEntity)
                .toList();

        ZoneId zone = resolveZoneId(zoneId);
        String filterSummary = buildInventoryFilterSummary(keyword, categoryId, supplierId, active, stockStatus, lowStockOnly, period, range);

        ExportFormat exportFormat = format != null ? format : ExportFormat.EXCEL;
        String timestamp = FILENAME_DATE_FORMATTER.format(Instant.now().atZone(zone));

        if (exportFormat == ExportFormat.PDF) {
            byte[] data = pdfReportService.generateInventoryReport(items, filterSummary, zone);
            return ExportedFile.builder()
                    .fileName("inventory_report_" + timestamp + ".pdf")
                    .contentType(PDF_CONTENT_TYPE)
                    .data(data)
                    .build();
        } else {
            byte[] data = excelReportService.generateInventoryReport(items, filterSummary, zone);
            return ExportedFile.builder()
                    .fileName("inventory_report_" + timestamp + ".xlsx")
                    .contentType(EXCEL_CONTENT_TYPE)
                    .data(data)
                    .build();
        }
    }

    @Override
    public ExportedFile exportStockTransactionReport(
            ExportFormat format,
            Long productId,
            TransactionType type,
            StockOutReason reason,
            Long userId,
            ReportPeriod period,
            Instant fromDate,
            Instant toDate,
            String zoneId
    ) {
        ReportDateRange range = ReportDateRange.resolve(period, fromDate, toDate, zoneId);

        List<StockTransaction> transactions = stockTransactionRepository.findTransactionsList(
                productId,
                type,
                reason,
                userId,
                range.getFromDate(),
                range.getToDate()
        );

        List<StockTransactionResponse> items = transactions.stream()
                .map(StockTransactionResponse::fromEntity)
                .toList();

        ZoneId zone = resolveZoneId(zoneId);
        String filterSummary = buildTransactionFilterSummary(productId, type, reason, userId, period, range);

        ExportFormat exportFormat = format != null ? format : ExportFormat.EXCEL;
        String timestamp = FILENAME_DATE_FORMATTER.format(Instant.now().atZone(zone));

        if (exportFormat == ExportFormat.PDF) {
            byte[] data = pdfReportService.generateStockTransactionReport(items, filterSummary, zone);
            return ExportedFile.builder()
                    .fileName("stock_transactions_" + timestamp + ".pdf")
                    .contentType(PDF_CONTENT_TYPE)
                    .data(data)
                    .build();
        } else {
            byte[] data = excelReportService.generateStockTransactionReport(items, filterSummary, zone);
            return ExportedFile.builder()
                    .fileName("stock_transactions_" + timestamp + ".xlsx")
                    .contentType(EXCEL_CONTENT_TYPE)
                    .data(data)
                    .build();
        }
    }

    private ZoneId resolveZoneId(String zoneIdString) {
        if (zoneIdString != null && !zoneIdString.trim().isEmpty()) {
            try {
                return ZoneId.of(zoneIdString.trim());
            } catch (Exception e) {
                log.warn("Invalid zoneId: '{}', falling back to system default", zoneIdString);
            }
        }
        return ZoneId.systemDefault();
    }

    private String buildInventoryFilterSummary(String keyword, Long categoryId, Long supplierId, Boolean active, StockStatusFilter stockStatus, Boolean lowStockOnly, ReportPeriod period, ReportDateRange range) {
        List<String> parts = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) parts.add("Keyword: '" + keyword + "'");
        if (categoryId != null) parts.add("Category ID: " + categoryId);
        if (supplierId != null) parts.add("Supplier ID: " + supplierId);
        if (active != null) parts.add("Status: " + (active ? "Active" : "Inactive"));
        if (stockStatus != null && stockStatus != StockStatusFilter.ALL) parts.add("Stock Status: " + stockStatus.name());
        if (Boolean.TRUE.equals(lowStockOnly)) parts.add("Low Stock Only: true");
        if (period != null) parts.add("Period: " + period.name());
        if (range != null && range.getFromDate() != null) parts.add("From: " + range.getFromDate());
        if (range != null && range.getToDate() != null) parts.add("To: " + range.getToDate());
        return parts.isEmpty() ? "All Products" : String.join(", ", parts);
    }

    private String buildTransactionFilterSummary(Long productId, TransactionType type, StockOutReason reason, Long userId, ReportPeriod period, ReportDateRange range) {
        List<String> parts = new ArrayList<>();
        if (period != null) parts.add("Period: " + period.name());
        if (productId != null) parts.add("Product ID: " + productId);
        if (type != null) parts.add("Type: " + type.name());
        if (reason != null) parts.add("Reason: " + reason.name());
        if (userId != null) parts.add("User ID: " + userId);
        if (range.getFromDate() != null) parts.add("From: " + range.getFromDate());
        if (range.getToDate() != null) parts.add("To: " + range.getToDate());
        return parts.isEmpty() ? "All Transactions" : String.join(", ", parts);
    }

    private static class CategoryAccumulator {
        private final Long categoryId;
        private final String categoryName;
        private long totalItems = 0;
        private long totalUnits = 0;
        private BigDecimal totalValuation = BigDecimal.ZERO;

        public CategoryAccumulator(Long categoryId, String categoryName) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
        }

        public void addItem(int units, BigDecimal valuation) {
            this.totalItems++;
            this.totalUnits += units;
            this.totalValuation = this.totalValuation.add(valuation);
        }

        public Long getCategoryId() { return categoryId; }
        public String getCategoryName() { return categoryName; }
        public long getTotalItems() { return totalItems; }
        public long getTotalUnits() { return totalUnits; }
        public BigDecimal getTotalValuation() { return totalValuation; }
    }
}
