package com.api.v1.inventory.controller;

import com.api.v1.inventory.common.pagination.PaginationValidator;
import com.api.v1.inventory.common.response.ApiResponse;
import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.enumerate.ExportFormat;
import com.api.v1.inventory.model.enumerate.ReportPeriod;
import com.api.v1.inventory.model.enumerate.StockStatusFilter;
import com.api.v1.inventory.model.enumerate.TransactionType;
import com.api.v1.inventory.model.response.ExportedFile;
import com.api.v1.inventory.model.response.InventoryReportPageResponse;
import com.api.v1.inventory.model.response.InventoryReportResponse;
import com.api.v1.inventory.model.response.InventorySummaryReportResponse;
import com.api.v1.inventory.model.response.StockTransactionResponse;
import com.api.v1.inventory.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
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
    @Operation(summary = "Get stock transactions report", description = "Query stock movement transactions with period presets (TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR) or custom date ranges.")
    public ResponseEntity<ApiResponse<PageResponse<StockTransactionResponse>>> getStockTransactionReport(
            @Parameter(description = "Filter transactions by product ID")
            @RequestParam(required = false) Long productId,
            @Parameter(description = "Filter by transaction type (STOCK_IN, STOCK_OUT)")
            @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Filter transactions by user ID who performed it")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "Predefined report period (TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR, CUSTOM). Defaults to CUSTOM.")
            @RequestParam(required = false) ReportPeriod period,
            @Parameter(description = "Filter transactions created on or after this timestamp (used when period is CUSTOM)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @Parameter(description = "Filter transactions created on or before this timestamp (used when period is CUSTOM)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @Parameter(description = "Client timezone (e.g. 'Asia/Phnom_Penh', 'UTC'). Defaults to server timezone.")
            @RequestParam(required = false) String zoneId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Pageable validatedPageable = PaginationValidator.validateAndStabilize(pageable, ALLOWED_STOCK_TRANSACTIONS_SORT_PROPERTIES, "id");
        PageResponse<StockTransactionResponse> response = reportService.getStockTransactionReport(
                productId, type, userId, period, fromDate, toDate, zoneId, validatedPageable
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Stock transaction report retrieved successfully"));
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Get inventory status report", description = "Query current inventory levels with totals (total products, total units, total valuation, in-stock/low-stock/out-of-stock counts) and filters for out-of-stock, low-stock, category, supplier, and date period.")
    public ResponseEntity<ApiResponse<InventoryReportPageResponse>> getInventoryReport(
            @Parameter(description = "Filter by product name or SKU (case-insensitive substring)")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter inventory by category ID")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter inventory by supplier ID")
            @RequestParam(required = false) Long supplierId,
            @Parameter(description = "Filter by active product status", schema = @Schema(type = "string"))
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Filter by stock status: ALL, IN_STOCK, LOW_STOCK, OUT_OF_STOCK")
            @RequestParam(required = false, defaultValue = "ALL") StockStatusFilter stockStatus,
            @Parameter(description = "Filter only products at or below minimum stock threshold")
            @RequestParam(required = false) Boolean lowStockOnly,
            @Parameter(description = "Predefined report period (TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR, CUSTOM)")
            @RequestParam(required = false) ReportPeriod period,
            @Parameter(description = "Filter products updated on or after this timestamp (used when period is CUSTOM)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @Parameter(description = "Filter products updated on or before this timestamp (used when period is CUSTOM)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @Parameter(description = "Client timezone (e.g. 'Asia/Phnom_Penh', 'UTC'). Defaults to server timezone.")
            @RequestParam(required = false) String zoneId,
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Pageable validatedPageable = PaginationValidator.validateAndStabilize(pageable, ALLOWED_INVENTORY_REPORT_SORT_PROPERTIES, "id");
        InventoryReportPageResponse response = reportService.getInventoryReport(
                keyword, categoryId, supplierId, active, stockStatus, lowStockOnly, period, fromDate, toDate, zoneId, validatedPageable
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Inventory report retrieved successfully"));
    }

    @GetMapping({"/inventory/summary", "/stock-summary"})
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(
            summary = "Get stock summary report and category breakdown",
            description = "Calculates stock summary (total items, total units, valuation, in-stock/low-stock/out-of-stock counts) and category stock breakdown based on the applied filters."
    )
    public ResponseEntity<ApiResponse<InventorySummaryReportResponse>> getInventorySummaryReport(
            @Parameter(description = "Filter by product name or SKU (case-insensitive substring)")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter inventory by category ID")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter inventory by supplier ID")
            @RequestParam(required = false) Long supplierId,
            @Parameter(description = "Filter by active product status", schema = @Schema(type = "string"))
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Filter by stock status: ALL, IN_STOCK, LOW_STOCK, OUT_OF_STOCK")
            @RequestParam(required = false, defaultValue = "ALL") StockStatusFilter stockStatus,
            @Parameter(description = "Filter only products at or below minimum stock threshold")
            @RequestParam(required = false) Boolean lowStockOnly,
            @Parameter(description = "Predefined report period (TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR, CUSTOM)")
            @RequestParam(required = false) ReportPeriod period,
            @Parameter(description = "Filter products updated on or after this timestamp (used when period is CUSTOM)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @Parameter(description = "Filter products updated on or before this timestamp (used when period is CUSTOM)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @Parameter(description = "Client timezone (e.g. 'Asia/Phnom_Penh', 'UTC'). Defaults to server timezone.")
            @RequestParam(required = false) String zoneId
    ) {
        InventorySummaryReportResponse response = reportService.getInventorySummaryReport(
                keyword, categoryId, supplierId, active, stockStatus, lowStockOnly, period, fromDate, toDate, zoneId
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Stock summary report retrieved successfully"));
    }

    @GetMapping("/inventory/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Export inventory report", description = "Export current inventory status, valuation, and category breakdown as an Excel (.xlsx) or PDF (.pdf) file.")
    public ResponseEntity<Resource> exportInventoryReport(
            @Parameter(description = "Export file format (EXCEL or PDF). Default is EXCEL.")
            @RequestParam(defaultValue = "EXCEL") ExportFormat format,
            @Parameter(description = "Filter by product name or SKU")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter inventory by category ID")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter inventory by supplier ID")
            @RequestParam(required = false) Long supplierId,
            @Parameter(description = "Filter by active product status")
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Filter by stock status: ALL, IN_STOCK, LOW_STOCK, OUT_OF_STOCK")
            @RequestParam(required = false, defaultValue = "ALL") StockStatusFilter stockStatus,
            @Parameter(description = "Legacy flag: if true, only include products at or below minimum stock threshold")
            @RequestParam(required = false) Boolean lowStockOnly,
            @Parameter(description = "Predefined report period (TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR, CUSTOM)")
            @RequestParam(required = false) ReportPeriod period,
            @Parameter(description = "Filter products updated on or after this timestamp (used when period is CUSTOM)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @Parameter(description = "Filter products updated on or before this timestamp (used when period is CUSTOM)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @Parameter(description = "Client timezone for timestamps. Defaults to server timezone.")
            @RequestParam(required = false) String zoneId
    ) {
        ExportedFile file = reportService.exportInventoryReport(
                format, keyword, categoryId, supplierId, active, stockStatus, lowStockOnly, period, fromDate, toDate, zoneId
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, file.getContentType())
                .body(new ByteArrayResource(file.getData()));
    }

    @GetMapping("/stock-transactions/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Export stock transactions report", description = "Export stock transactions report as an Excel (.xlsx) or PDF (.pdf) file.")
    public ResponseEntity<Resource> exportStockTransactions(
            @Parameter(description = "Export file format (EXCEL or PDF). Default is EXCEL.")
            @RequestParam(defaultValue = "EXCEL") ExportFormat format,
            @Parameter(description = "Filter transactions by product ID")
            @RequestParam(required = false) Long productId,
            @Parameter(description = "Filter by transaction type (STOCK_IN, STOCK_OUT)")
            @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Filter transactions by user ID who performed it")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "Predefined report period (TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR, CUSTOM). Default is THIS_MONTH.")
            @RequestParam(defaultValue = "THIS_MONTH") ReportPeriod period,
            @Parameter(description = "Filter transactions created on or after this timestamp (used when period is CUSTOM)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @Parameter(description = "Filter transactions created on or before this timestamp (used when period is CUSTOM)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @Parameter(description = "Client timezone for timestamps. Defaults to server timezone.")
            @RequestParam(required = false) String zoneId
    ) {
        ExportedFile file = reportService.exportStockTransactionReport(
                format, productId, type, userId, period, fromDate, toDate, zoneId
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, file.getContentType())
                .body(new ByteArrayResource(file.getData()));
    }
}
