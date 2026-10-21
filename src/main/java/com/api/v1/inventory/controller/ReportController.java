package com.api.v1.inventory.controller;

import com.api.v1.inventory.common.response.ApiResponse;
import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.enumerate.TransactionType;
import com.api.v1.inventory.model.response.InventoryReportResponse;
import com.api.v1.inventory.model.response.StockTransactionResponse;
import com.api.v1.inventory.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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

import com.api.v1.inventory.common.pagination.PaginationValidator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports")
public class ReportController {

    public static final List<String> ALLOWED_STOCK_TRANSACTIONS_SORT_PROPERTIES = List.of(
            "id", "quantity", "transactionType", "createdAt"
    );

    public static final List<String> ALLOWED_INVENTORY_REPORT_SORT_PROPERTIES = List.of(
            "id", "sku", "name", "stockQuantity", "unitPrice", "createdAt"
    );

    private final ReportService reportService;

    @GetMapping("/stock-transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<PageResponse<StockTransactionResponse>>> getStockTransactionReport(
            @Parameter(description = "Filter transactions by product ID")
            @RequestParam(required = false) Long productId,
            @Parameter(description = "Filter by transaction type (e.g. IN, OUT)")
            @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Filter transactions by user ID who performed it")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "Filter transactions created on or after this timestamp")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @Parameter(description = "Filter transactions created on or before this timestamp")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Pageable validatedPageable = PaginationValidator.validateAndStabilize(pageable, ALLOWED_STOCK_TRANSACTIONS_SORT_PROPERTIES, "id");
        PageResponse<StockTransactionResponse> response = reportService.getStockTransactionReport(
                productId, type, userId, fromDate, toDate, validatedPageable
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Stock transaction report retrieved successfully"));
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<PageResponse<InventoryReportResponse>>> getInventoryReport(
            @Parameter(description = "Filter by product name or SKU (case-insensitive substring)")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter inventory by category ID")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter inventory by supplier ID")
            @RequestParam(required = false) Long supplierId,
            @Parameter(description = "Filter by active product status", schema = @Schema(type = "string"))
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "If true, only include products at or below their minimum stock threshold")
            @RequestParam(required = false) Boolean lowStockOnly,
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Pageable validatedPageable = PaginationValidator.validateAndStabilize(pageable, ALLOWED_INVENTORY_REPORT_SORT_PROPERTIES, "id");
        PageResponse<InventoryReportResponse> response = reportService.getInventoryReport(
                keyword, categoryId, supplierId, active, lowStockOnly, validatedPageable
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Inventory report retrieved successfully"));
    }
}
