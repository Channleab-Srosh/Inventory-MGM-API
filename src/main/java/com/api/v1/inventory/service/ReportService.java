package com.api.v1.inventory.service;

import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.enumerate.TransactionType;
import com.api.v1.inventory.model.response.InventoryReportResponse;
import com.api.v1.inventory.model.response.StockTransactionResponse;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface ReportService {

    PageResponse<StockTransactionResponse> getStockTransactionReport(
            Long productId,
            TransactionType type,
            Long userId,
            Instant fromDate,
            Instant toDate,
            Pageable pageable
    );

    PageResponse<InventoryReportResponse> getInventoryReport(
            String keyword,
            Long categoryId,
            Long supplierId,
            Boolean active,
            Boolean lowStockOnly,
            Pageable pageable
    );
}
