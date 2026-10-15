package com.api.v1.inventory.service.impl;

import com.api.v1.inventory.common.exception.BadRequestException;
import com.api.v1.inventory.config.AppProperty;
import com.api.v1.inventory.model.entity.AppUser;
import com.api.v1.inventory.model.entity.AppUserToken;
import com.api.v1.inventory.model.enumerate.Role;
import com.api.v1.inventory.model.request.ForgotPasswordRequest;
import com.api.v1.inventory.model.request.LoginRequest;
import com.api.v1.inventory.model.request.ResetPasswordRequest;
import com.api.v1.inventory.model.response.AuthResponse;
import com.api.v1.inventory.repository.AppUserRepository;
import com.api.v1.inventory.repository.AppUserTokenRepository;
import com.api.v1.inventory.security.JwtService;
import com.api.v1.inventory.service.AppUserService;
import com.api.v1.inventory.service.AuthService;
import com.api.v1.inventory.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserService appUserService;
    private final AppUserRepository userRepository;
    private final AppUserTokenRepository userTokenRepository;
    private final JwtService jwtService;
    private final AppProperty appProperty;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public AuthResponse login(LoginRequest request) {
        String username = request.getUsername().trim();

        AppUser user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new BadCredentialsException("Username does not exist"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Account is disabled. Please contact administrator.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Incorrect password");
        }

        UserDetails userDetails = appUserService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails, user.getRole().name());
        long expiresIn = appProperty.getJwt().getAccessExpiration() / 1000;

        log.info("User logged in successfully: username={}, role={}", user.getUsername(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .unit("seconds")
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Optional<AppUser> userOpt = userRepository.findByEmailIgnoreCase(email);

        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existing email: {}", email);
            return;
        }

        AppUser user = userOpt.get();

        if (user.getRole() == Role.ADMIN) {
            log.warn("Password reset requested for ADMIN account ({}). Admin self-service reset is restricted.", user.getUsername());
            return;
        }

        if (!user.isActive()) {
            log.warn("Password reset requested for disabled account: {}", user.getUsername());
            return;
        }

        // Generate 6-digit numeric OTP
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));

        userTokenRepository.deleteByUserId(user.getId());

        AppUserToken tokenEntity = AppUserToken.builder()
                .user(user)
                .token(otp)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .revoked(false)
                .build();
        userTokenRepository.save(tokenEntity);

        emailService.sendPasswordResetEmail(user.getEmail(), otp);
        log.info("Issued password reset OTP for user: id={}, email={}", user.getId(), user.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenStr = request.getToken().trim();
        AppUserToken tokenEntity = userTokenRepository.findByTokenAndRevokedFalse(tokenStr)
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token"));

        if (tokenEntity.getExpiresAt().isBefore(Instant.now())) {
            tokenEntity.setRevoked(true);
            userTokenRepository.save(tokenEntity);
            throw new BadRequestException("Password reset token has expired. Please request a new one.");
        }

        AppUser user = tokenEntity.getUser();
        if (user.getRole() == Role.ADMIN) {
            tokenEntity.setRevoked(true);
            userTokenRepository.save(tokenEntity);
            throw new BadRequestException("Administrator accounts cannot be reset via self-service.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        tokenEntity.setRevoked(true);
        userTokenRepository.save(tokenEntity);

        log.info("Password successfully reset for user id={}, username={}", user.getId(), user.getUsername());
    }
}
