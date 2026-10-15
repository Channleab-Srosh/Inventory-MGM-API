package com.api.v1.inventory.service;

import com.api.v1.inventory.model.request.LoginRequest;
import com.api.v1.inventory.model.response.AuthResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);
}
