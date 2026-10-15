package com.api.v1.inventory.controller;

import com.api.v1.inventory.common.response.ApiResponse;
import com.api.v1.inventory.model.request.LoginRequest;
import com.api.v1.inventory.model.response.AuthResponse;
import com.api.v1.inventory.model.response.UserResponse;
import com.api.v1.inventory.service.AppUserService;
import com.api.v1.inventory.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
//@Tag(name = "Authentication", description = "Authentication and user identity endpoints")
public class AuthController {


    private final AuthService authService;
    private final AppUserService appUserService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and get JWT access token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(authResponse, "Login successful"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse userResponse = appUserService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.ok(userResponse, "Current user profile retrieved successfully"));
    }
}
