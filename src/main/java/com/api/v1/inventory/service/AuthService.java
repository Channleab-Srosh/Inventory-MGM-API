package com.api.v1.inventory.service;

import com.api.v1.inventory.model.request.*;
import com.api.v1.inventory.model.response.AuthResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    void sendRegistrationOtp(SendRegistrationOtpRequest request);

    AuthResponse register(RegisterRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(ChangePasswordRequest request);
}
