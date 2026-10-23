package com.api.v1.inventory.controller;

import com.api.v1.inventory.common.pagination.PaginationValidator;
import com.api.v1.inventory.common.util.IdListParser;
import com.api.v1.inventory.common.response.ApiResponse;
import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.request.CreateCustomerRequest;
import com.api.v1.inventory.model.request.UpdateCustomerRequest;
import com.api.v1.inventory.model.request.UpdateCustomerStatusRequest;
import com.api.v1.inventory.model.response.CustomerResponse;
import com.api.v1.inventory.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management")
public class CustomerController {

    public static final List<String> ALLOWED_SORT_PROPERTIES = List.of(
            "id", "name", "phone", "email", "address", "active", "createdAt", "updatedAt"
    );

    private final CustomerService customerService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request
    ) {
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Customer created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> getAllCustomers(
            @Parameter(description = "Filter customers by name or phone (case-insensitive substring)")
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Pageable validatedPageable = PaginationValidator.validateAndStabilize(pageable, ALLOWED_SORT_PROPERTIES, "id");
        PageResponse<CustomerResponse> response = customerService.getAllCustomers(search, validatedPageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Customers retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable Long id) {
        CustomerResponse response = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Customer retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request
    ) {
        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Customer updated successfully"));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomerStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerStatusRequest request
    ) {
        CustomerResponse response = customerService.updateCustomerStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Customer status updated successfully"));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete single or multiple customers by ID",
            description = "Deletes a single customer or multiple customers given as comma-separated IDs (e.g. '1' or '1,2,3'). Max 100 IDs. Atomic operation."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Customer/Customers deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid IDs format, non-numeric, negative, zero, empty, or exceeds 100 IDs"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "One or more customer IDs not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteCustomers(
            @Parameter(description = "Single ID or comma-separated IDs", example = "1,2,3")
            @PathVariable String ids) {
        List<Long> idList = IdListParser.parse(ids);
        customerService.deleteCustomers(idList);
        String message = idList.size() > 1 ? "Customers deleted successfully" : "Customer deleted successfully";
        return ResponseEntity.ok(ApiResponse.ok(null, message));
    }
}
