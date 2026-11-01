package com.api.v1.inventory.controller;

import com.api.v1.inventory.common.response.ApiResponse;
import com.api.v1.inventory.model.request.*;
import com.api.v1.inventory.model.response.AuthResponse;
import com.api.v1.inventory.model.response.UserResponse;
import com.api.v1.inventory.service.AppUserService;
import com.api.v1.inventory.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final AppUserService appUserService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(authResponse, "Login successful"));
    }

    @PostMapping("/send-registration-otp")
    public ResponseEntity<ApiResponse<Void>> sendRegistrationOtp(@Valid @RequestBody SendRegistrationOtpRequest request) {
        authService.sendRegistrationOtp(request);
        return ResponseEntity.ok(ApiResponse.ok(
                null,
                "Verification code (OTP) has been sent to your Gmail address."
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse authResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(authResponse, "Account registered successfully. Your account is pending administrator approval before you can log in."));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieves the authenticated user's profile details.")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse userResponse = appUserService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.ok(userResponse, "Current user profile retrieved successfully"));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Allows any authenticated user to update their full name, email, and phone number.")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUserProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UserResponse userResponse = appUserService.updateCurrentUserProfile(request);
        return ResponseEntity.ok(ApiResponse.ok(userResponse, "Profile updated successfully"));
    }

    @PostMapping(value = "/me/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload current user profile image",
            description = "Uploads an avatar/profile picture (JPEG, PNG, GIF, WEBP, or JFIF; max 5 MB) for the currently authenticated user."
    )
    public ResponseEntity<ApiResponse<UserResponse>> uploadCurrentUserProfileImage(
            @RequestParam("file") MultipartFile file
    ) {
        UserResponse userResponse = appUserService.uploadCurrentUserProfileImage(file);
        return ResponseEntity.ok(ApiResponse.ok(userResponse, "Profile image uploaded successfully"));
    }

    @DeleteMapping("/me/image")
    @Operation(summary = "Remove current user profile image", description = "Removes the avatar/profile picture for the currently authenticated user.")
    public ResponseEntity<ApiResponse<UserResponse>> removeCurrentUserProfileImage() {
        UserResponse userResponse = appUserService.removeCurrentUserProfileImage();
        return ResponseEntity.ok(ApiResponse.ok(userResponse, "Profile image removed successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(
                null,
                "Password reset instructions have been sent to " + request.getEmail().trim() + "."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(
                null,
                "Password reset successfully. You can now log in with your new password."
        ));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody com.api.v1.inventory.model.request.ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.ok(
                null,
                "Password changed successfully."
        ));
    }
}
