package com.api.v1.inventory.controller;

import com.api.v1.inventory.common.response.ApiResponse;
import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.response.DashboardSummaryResponse;
import com.api.v1.inventory.model.response.ProductResponse;
import com.api.v1.inventory.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.v1.inventory.common.pagination.PaginationValidator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Endpoints for dashboard analytics, KPI metrics, and low-stock monitoring")
public class DashboardController {

    public static final List<String> ALLOWED_SORT_PROPERTIES = List.of(
            "stockQuantity", "id", "name", "minimumStock", "unitPrice"
    );

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get aggregate dashboard KPI summary metrics (Admin and Staff)")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        DashboardSummaryResponse response = dashboardService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.ok(response, "Dashboard summary retrieved successfully"));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get paginated list of products at or below their minimum stock threshold (Admin and Staff)")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getLowStock(
            @ParameterObject @PageableDefault(size = 10, sort = "stockQuantity", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Pageable validatedPageable = PaginationValidator.validateAndStabilize(pageable, ALLOWED_SORT_PROPERTIES, "id");
        PageResponse<ProductResponse> response = dashboardService.getLowStockProducts(validatedPageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Low stock products retrieved successfully"));
    }
}
