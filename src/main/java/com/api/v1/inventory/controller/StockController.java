package com.api.v1.inventory.controller;

import com.api.v1.inventory.common.response.ApiResponse;
import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.enumerate.TransactionType;
import com.api.v1.inventory.model.request.StockOperationRequest;
import com.api.v1.inventory.model.response.StockTransactionResponse;
import com.api.v1.inventory.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
@Tag(name = "Stock Operations", description = "Endpoints for stock management and transaction audit history")
public class StockController {

    private final StockService stockService;

    @PostMapping("/in")
    @Operation(summary = "Perform Stock In operation (Admin and Staff)")
    public ResponseEntity<ApiResponse<StockTransactionResponse>> stockIn(
            @Valid @RequestBody StockOperationRequest request
    ) {
        StockTransactionResponse response = stockService.stockIn(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Stock added successfully"));
    }

    @PostMapping("/out")
    @Operation(summary = "Perform Stock Out operation with availability check (Admin and Staff)")
    public ResponseEntity<ApiResponse<StockTransactionResponse>> stockOut(
            @Valid @RequestBody StockOperationRequest request
    ) {
        StockTransactionResponse response = stockService.stockOut(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Stock deducted successfully"));
    }

    @GetMapping("/history")
    @Operation(summary = "Get transaction audit history with filters and pagination (Admin and Staff)")
    public ResponseEntity<ApiResponse<PageResponse<StockTransactionResponse>>> getHistory(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<StockTransactionResponse> response = stockService.getTransactionHistory(
                productId, type, userId, fromDate, toDate, pageable
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Stock history retrieved successfully"));
    }

    @GetMapping("/history/{id}")
    @Operation(summary = "Get transaction details by ID (Admin and Staff)")
    public ResponseEntity<ApiResponse<StockTransactionResponse>> getHistoryById(@PathVariable Long id) {
        StockTransactionResponse response = stockService.getTransactionById(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Stock transaction retrieved successfully"));
    }
}
