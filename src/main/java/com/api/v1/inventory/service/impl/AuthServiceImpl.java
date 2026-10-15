package com.api.v1.inventory.service.impl;

import com.api.v1.inventory.config.AppProperty;
import com.api.v1.inventory.model.entity.AppUser;
import com.api.v1.inventory.model.request.LoginRequest;
import com.api.v1.inventory.model.response.AuthResponse;
import com.api.v1.inventory.repository.AppUserRepository;
import com.api.v1.inventory.security.JwtService;
import com.api.v1.inventory.service.AppUserService;
import com.api.v1.inventory.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserService appUserService;
    private final AppUserRepository userRepository;
    private final JwtService jwtService;
    private final AppProperty appProperty;

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword())
        );

        UserDetails userDetails = appUserService.loadUserByUsername(request.getUsername().trim());
        AppUser user = userRepository.findByUsernameIgnoreCase(request.getUsername().trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + request.getUsername()));

        String token = jwtService.generateToken(userDetails, user.getRole().name());
        long expiresIn = appProperty.getJwt().getAccessExpiration() / 1000;

        log.info("User logged in successfully: username={}, role={}", user.getUsername(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }
}
