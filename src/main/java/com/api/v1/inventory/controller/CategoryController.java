package com.api.v1.inventory.controller;

import com.api.v1.inventory.common.response.ApiResponse;
import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.request.CreateCategoryRequest;
import com.api.v1.inventory.model.request.UpdateCategoryRequest;
import com.api.v1.inventory.model.response.CategoryResponse;
import com.api.v1.inventory.service.CategoryService;
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
import com.api.v1.inventory.common.util.IdListParser;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Category Management")
public class CategoryController {

    public static final List<String> ALLOWED_SORT_PROPERTIES = List.of(
            "id", "name", "createdAt", "updatedAt"
    );

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Category created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getAllCategories(
            @Parameter(description = "Filter categories by name (case-insensitive substring)")
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Pageable validatedPageable = PaginationValidator.validateAndStabilize(pageable, ALLOWED_SORT_PROPERTIES, "id");
        PageResponse<CategoryResponse> response = categoryService.getAllCategories(search, validatedPageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Categories retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        CategoryResponse response = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Category retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        CategoryResponse response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Category updated successfully"));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
            summary = "Delete single or multiple categories by ID",
            description = "Deletes a single category or multiple categories given as comma-separated IDs (e.g. '1' or '1,2,3'). Max 100 IDs. Atomic operation."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category/Categories deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid IDs format, non-numeric, negative, zero, empty, or exceeds 100 IDs"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or MANAGER role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "One or more category IDs not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "One or more categories are referenced by products")
    })
    public ResponseEntity<ApiResponse<Void>> deleteCategories(
            @Parameter(description = "Single ID or comma-separated IDs", example = "1,2,3")
            @PathVariable String ids) {
        List<Long> idList = IdListParser.parse(ids);
        categoryService.deleteCategories(idList);
        String message = idList.size() > 1 ? "Categories deleted successfully" : "Category deleted successfully";
        return ResponseEntity.ok(ApiResponse.ok(null, message));
    }
}
