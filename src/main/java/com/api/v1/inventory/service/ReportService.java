package com.api.v1.inventory.service;

import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.enumerate.ExportFormat;
import com.api.v1.inventory.model.enumerate.ReportPeriod;
import com.api.v1.inventory.model.enumerate.StockStatusFilter;
import com.api.v1.inventory.model.enumerate.TransactionType;
import com.api.v1.inventory.model.response.ExportedFile;
import com.api.v1.inventory.model.response.InventoryReportResponse;
import com.api.v1.inventory.model.response.InventorySummaryReportResponse;
import com.api.v1.inventory.model.response.StockTransactionResponse;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface ReportService {

    PageResponse<StockTransactionResponse> getStockTransactionReport(
            Long productId,
            TransactionType type,
            Long userId,
            ReportPeriod period,
            Instant fromDate,
            Instant toDate,
            String zoneId,
            Pageable pageable
    );

    default PageResponse<StockTransactionResponse> getStockTransactionReport(
            Long productId,
            TransactionType type,
            Long userId,
            Instant fromDate,
            Instant toDate,
            Pageable pageable
    ) {
        return getStockTransactionReport(productId, type, userId, ReportPeriod.CUSTOM, fromDate, toDate, null, pageable);
    }

    PageResponse<InventoryReportResponse> getInventoryReport(
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
    );

    default PageResponse<InventoryReportResponse> getInventoryReport(
            String keyword,
            Long categoryId,
            Long supplierId,
            Boolean active,
            StockStatusFilter stockStatus,
            Boolean lowStockOnly,
            Pageable pageable
    ) {
        return getInventoryReport(keyword, categoryId, supplierId, active, stockStatus, lowStockOnly, null, null, null, null, pageable);
    }

    default PageResponse<InventoryReportResponse> getInventoryReport(
            String keyword,
            Long categoryId,
            Long supplierId,
            Boolean active,
            Boolean lowStockOnly,
            Pageable pageable
    ) {
        return getInventoryReport(keyword, categoryId, supplierId, active, StockStatusFilter.ALL, lowStockOnly, pageable);
    }

    InventorySummaryReportResponse getInventorySummaryReport(
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
    );

    default InventorySummaryReportResponse getInventorySummaryReport(
            String keyword,
            Long categoryId,
            Long supplierId,
            Boolean active,
            StockStatusFilter stockStatus,
            Boolean lowStockOnly
    ) {
        return getInventorySummaryReport(keyword, categoryId, supplierId, active, stockStatus, lowStockOnly, null, null, null, null);
    }

    ExportedFile exportInventoryReport(
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
    );

    default ExportedFile exportInventoryReport(
            ExportFormat format,
            String keyword,
            Long categoryId,
            Long supplierId,
            Boolean active,
            StockStatusFilter stockStatus,
            Boolean lowStockOnly,
            String zoneId
    ) {
        return exportInventoryReport(format, keyword, categoryId, supplierId, active, stockStatus, lowStockOnly, null, null, null, zoneId);
    }

    ExportedFile exportStockTransactionReport(
            ExportFormat format,
            Long productId,
            TransactionType type,
            Long userId,
            ReportPeriod period,
            Instant fromDate,
            Instant toDate,
            String zoneId
    );
}
