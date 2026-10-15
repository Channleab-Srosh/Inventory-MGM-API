package com.api.v1.inventory.service;

import com.api.v1.inventory.model.request.ChangePasswordRequest;
import com.api.v1.inventory.model.request.ForgotPasswordRequest;
import com.api.v1.inventory.model.request.LoginRequest;
import com.api.v1.inventory.model.request.ResetPasswordRequest;
import com.api.v1.inventory.model.response.AuthResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(ChangePasswordRequest request);
}
