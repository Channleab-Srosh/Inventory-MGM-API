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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

import com.api.v1.inventory.common.pagination.PaginationValidator;
import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
@Tag(name = "Stock Operations")
public class StockController {

    public static final List<String> ALLOWED_SORT_PROPERTIES = List.of(
            "id", "quantity", "transactionType", "createdAt"
    );

    private final StockService stockService;

    @PostMapping("/in")
    public ResponseEntity<ApiResponse<StockTransactionResponse>> stockIn(
            @Valid @RequestBody StockOperationRequest request
    ) {
        StockTransactionResponse response = stockService.stockIn(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Stock added successfully"));
    }

    @PostMapping("/out")
    public ResponseEntity<ApiResponse<StockTransactionResponse>> stockOut(
            @Valid @RequestBody StockOperationRequest request
    ) {
        StockTransactionResponse response = stockService.stockOut(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Stock deducted successfully"));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PageResponse<StockTransactionResponse>>> getHistory(
            @Parameter(description = "Filter transactions by product ID")
            @RequestParam(required = false) Long productId,
            @Parameter(description = "Filter by transaction type")
            @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Filter transactions by user ID who performed it")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "Filter transactions created on or after this timestamp")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @Parameter(description = "Filter transactions created on or before this timestamp")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Pageable validatedPageable = PaginationValidator.validateAndStabilize(pageable, ALLOWED_SORT_PROPERTIES, "id");
        PageResponse<StockTransactionResponse> response = stockService.getTransactionHistory(
                productId, type, userId, fromDate, toDate, validatedPageable
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Stock history retrieved successfully"));
    }


    @GetMapping("/history/{id}")
    public ResponseEntity<ApiResponse<StockTransactionResponse>> getHistoryById(@PathVariable Long id) {
        StockTransactionResponse response = stockService.getTransactionById(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Stock transaction retrieved successfully"));
    }
}
