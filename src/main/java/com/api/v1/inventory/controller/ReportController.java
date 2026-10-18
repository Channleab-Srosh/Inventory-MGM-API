package com.api.v1.inventory.controller;

import com.api.v1.inventory.common.response.ApiResponse;
import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.enumerate.TransactionType;
import com.api.v1.inventory.model.response.InventoryReportResponse;
import com.api.v1.inventory.model.response.StockTransactionResponse;
import com.api.v1.inventory.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Endpoints for generating inventory and transaction audit reports")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/stock-transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get filtered, paginated stock transaction report (Admin and Staff)")
    public ResponseEntity<ApiResponse<PageResponse<StockTransactionResponse>>> getStockTransactionReport(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<StockTransactionResponse> response = reportService.getStockTransactionReport(
                productId, type, userId, fromDate, toDate, pageable
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Stock transaction report retrieved successfully"));
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get paginated product inventory valuation snapshot report (Admin and Staff)")
    public ResponseEntity<ApiResponse<PageResponse<InventoryReportResponse>>> getInventoryReport(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean lowStockOnly,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<InventoryReportResponse> response = reportService.getInventoryReport(
                keyword, categoryId, supplierId, active, lowStockOnly, pageable
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Inventory report retrieved successfully"));
    }
}
