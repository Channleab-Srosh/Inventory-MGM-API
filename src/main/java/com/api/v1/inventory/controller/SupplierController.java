package com.api.v1.inventory.controller;

import com.api.v1.inventory.common.response.ApiResponse;
import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.request.CreateSupplierRequest;
import com.api.v1.inventory.model.request.UpdateSupplierRequest;
import com.api.v1.inventory.model.response.SupplierResponse;
import com.api.v1.inventory.service.SupplierService;
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
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@Tag(name = "Supplier Management")
public class SupplierController {

    public static final List<String> ALLOWED_SORT_PROPERTIES = List.of(
            "id", "name", "phone", "email", "address", "createdAt", "updatedAt"
    );

    private final SupplierService supplierService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<SupplierResponse>> createSupplier(
            @Valid @RequestBody CreateSupplierRequest request
    ) {
        SupplierResponse response = supplierService.createSupplier(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Supplier created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SupplierResponse>>> getAllSuppliers(
            @Parameter(description = "Filter suppliers by name or phone (substring)")
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Pageable validatedPageable = PaginationValidator.validateAndStabilize(pageable, ALLOWED_SORT_PROPERTIES, "id");
        PageResponse<SupplierResponse> response = supplierService.getAllSuppliers(search, validatedPageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Suppliers retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponse>> getSupplierById(@PathVariable Long id) {
        SupplierResponse response = supplierService.getSupplierById(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Supplier retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<SupplierResponse>> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSupplierRequest request
    ) {
        SupplierResponse response = supplierService.updateSupplier(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Supplier updated successfully"));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
            summary = "Delete single or multiple suppliers by ID",
            description = "Deletes a single supplier or multiple suppliers given as comma-separated IDs (e.g. '1' or '1,2,3'). Max 100 IDs. Atomic operation."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Supplier/Suppliers deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid IDs format, non-numeric, negative, zero, empty, or exceeds 100 IDs"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or MANAGER role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "One or more supplier IDs not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "One or more suppliers are referenced by products")
    })
    public ResponseEntity<ApiResponse<Void>> deleteSuppliers(
            @Parameter(description = "Single ID or comma-separated IDs", example = "1,2,3")
            @PathVariable String ids) {
        List<Long> idList = IdListParser.parse(ids);
        supplierService.deleteSuppliers(idList);
        String message = idList.size() > 1 ? "Suppliers deleted successfully" : "Supplier deleted successfully";
        return ResponseEntity.ok(ApiResponse.ok(null, message));
    }
}
