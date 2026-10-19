package com.api.v1.inventory.controller;

import com.api.v1.inventory.common.response.ApiResponse;
import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.request.CreateProductRequest;
import com.api.v1.inventory.model.request.UpdateProductRequest;
import com.api.v1.inventory.model.response.ProductResponse;
import com.api.v1.inventory.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.api.v1.inventory.common.pagination.PaginationValidator;
import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Management")
public class ProductController {

    public static final List<String> ALLOWED_SORT_PROPERTIES = List.of(
            "id", "sku", "name", "unitPrice", "stockQuantity", "minimumStock", "active", "createdAt", "updatedAt"
    );

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Product created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(
            @Parameter(description = "Filter by product name or SKU (case-insensitive substring)")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter products by category ID")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter products by supplier ID")
            @RequestParam(required = false) Long supplierId,
            @Parameter(description = "Filter by active status")
            @RequestParam(required = false) Boolean active,
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Pageable validatedPageable = PaginationValidator.validateAndStabilize(pageable, ALLOWED_SORT_PROPERTIES, "id");
        PageResponse<ProductResponse> response = productService.getProducts(
                keyword,
                categoryId,
                supplierId,
                active,
                validatedPageable
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Products retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Product retrieved successfully"));
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySku(@PathVariable String sku) {
        ProductResponse response = productService.getProductBySku(sku);
        return ResponseEntity.ok(ApiResponse.ok(response, "Product retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Product updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Product deleted successfully"));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductStatus(
            @PathVariable Long id,
            @Valid @RequestBody com.api.v1.inventory.model.request.UpdateProductStatusRequest request
    ) {
        ProductResponse response = productService.updateProductStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Product status updated successfully"));
    }
}
