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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.api.v1.inventory.common.pagination.PaginationValidator;
import com.api.v1.inventory.common.util.IdListParser;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
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
            @Parameter(description = "Filter by active status", schema = @Schema(type = "string"))
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Product updated successfully"));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
            summary = "Delete single or multiple products by ID",
            description = "Deletes a single product or multiple products given as comma-separated IDs (e.g. '1' or '1,2,3'). Max 100 IDs. Atomic operation."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product/Products deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid IDs format, non-numeric, negative, zero, empty, or exceeds 100 IDs"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or MANAGER role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "One or more product IDs not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "One or more products have transaction records or non-zero stock")
    })
    public ResponseEntity<ApiResponse<Void>> deleteProducts(
            @Parameter(description = "Single ID or comma-separated IDs", example = "1,2,3")
            @PathVariable String ids) {
        List<Long> idList = IdListParser.parse(ids);
        productService.deleteProducts(idList);
        String message = idList.size() > 1 ? "Products deleted successfully" : "Product deleted successfully";
        return ResponseEntity.ok(ApiResponse.ok(null, message));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductStatus(
            @PathVariable Long id,
            @Valid @RequestBody com.api.v1.inventory.model.request.UpdateProductStatusRequest request
    ) {
        ProductResponse response = productService.updateProductStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Product status updated successfully"));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
            summary = "Upload product image",
            description = "Uploads an image file (JPEG, PNG, GIF, or WEBP; max 5 MB) for the given product. " +
                    "Replaces any previously uploaded image. The response contains the updated product with the new imageUrl."
    )
    public ResponseEntity<ApiResponse<ProductResponse>> uploadProductImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        ProductResponse response = productService.uploadProductImage(id, file);
        return ResponseEntity.ok(ApiResponse.ok(response, "Product image uploaded successfully"));
    }
}
