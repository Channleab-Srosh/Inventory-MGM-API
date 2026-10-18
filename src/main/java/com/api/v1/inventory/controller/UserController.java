package com.api.v1.inventory.controller;

import com.api.v1.inventory.common.response.ApiResponse;
import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.request.CreateUserRequest;
import com.api.v1.inventory.model.request.UpdateUserRequest;
import com.api.v1.inventory.model.request.UpdateUserStatusRequest;
import com.api.v1.inventory.model.response.UserResponse;
import com.api.v1.inventory.service.AppUserService;
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

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Admin-only user management endpoints")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final AppUserService appUserService;

    @PostMapping
    @Operation(summary = "Create a new user account (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = appUserService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "User created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all users with search and pagination (Admin only)")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<UserResponse> response = appUserService.getAllUsers(search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Users retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user details by ID (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse response = appUserService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "User retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing user (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        UserResponse response = appUserService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "User updated successfully"));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Enable or disable a user account (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        UserResponse response = appUserService.updateUserStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "User status updated successfully"));
    }
}
