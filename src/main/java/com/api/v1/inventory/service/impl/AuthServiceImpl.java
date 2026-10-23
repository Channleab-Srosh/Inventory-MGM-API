package com.api.v1.inventory.service.impl;

import com.api.v1.inventory.common.exception.BadRequestException;
import com.api.v1.inventory.common.exception.ConflictException;
import com.api.v1.inventory.config.AppProperty;
import com.api.v1.inventory.model.entity.AppUser;
import com.api.v1.inventory.model.entity.AppUserToken;
import com.api.v1.inventory.model.enumerate.Role;
import com.api.v1.inventory.model.request.*;
import com.api.v1.inventory.model.response.AuthResponse;
import com.api.v1.inventory.repository.AppUserRepository;
import com.api.v1.inventory.repository.AppUserTokenRepository;
import com.api.v1.inventory.security.CurrentUserService;
import com.api.v1.inventory.security.JwtService;
import com.api.v1.inventory.service.AppUserService;
import com.api.v1.inventory.service.AuthService;
import com.api.v1.inventory.service.EmailService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
    private final CurrentUserService currentUserService;
    private final SecureRandom secureRandom = new SecureRandom();

    /** In-memory cache entry for registration OTP */
    private static class RegistrationOtpEntry {
        final String otp;
        final Instant requestedAt;
        int attempts;

        RegistrationOtpEntry(String otp) {
            this.otp = otp;
            this.requestedAt = Instant.now();
            this.attempts = 0;
        }
    }

    /**
     * In-memory Caffeine cache storing transient registration OTPs.
     * Automatically evicts expired OTP entries after the configured expiration window.
     * Zero database overhead, sub-microsecond access, and zero disk/table bloat.
     */
    private final Cache<String, RegistrationOtpEntry> registrationOtpCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(AppProperty.OTP_EXPIRY_MINUTES))
                    .maximumSize(10_000)
                    .build();

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
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }

    @Override
    public void sendRegistrationOtp(SendRegistrationOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email '" + request.getEmail() + "' is already registered");
        }

        // ---- Resend rate-limit: at most 1 request per limitWindow seconds ----
        int limitWindowSeconds = appProperty.getOtp().getLimitWindow() > 0
                ? appProperty.getOtp().getLimitWindow()
                : 60;

        RegistrationOtpEntry existing = registrationOtpCache.getIfPresent(email);
        if (existing != null) {
            long elapsedSeconds = Duration.between(existing.requestedAt, Instant.now()).getSeconds();
            if (elapsedSeconds < limitWindowSeconds) {
                throw new BadRequestException(
                        "A verification code was already sent. Please wait " + (limitWindowSeconds - elapsedSeconds)
                                + " seconds before requesting a new one.");
            }
        }

        // ---- Generate a cryptographically secure 6-digit OTP ----
        String rawOtp = String.format("%06d", secureRandom.nextInt(1_000_000));

        // ---- Store directly in Caffeine cache (in-memory, auto-expiring in 5 minutes) ----
        registrationOtpCache.put(email, new RegistrationOtpEntry(rawOtp));

        // Pass the email as the display name since we don't know the full name yet
        emailService.sendRegistrationOtpEmail(email, email, rawOtp);

        log.info("Issued registration OTP for email: {}", email);
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String normalizedUsername = username.toLowerCase();
        String email = request.getEmail().trim().toLowerCase();
        String otp = request.getOtp().trim();

        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new ConflictException("Username '" + request.getUsername() + "' is already taken");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email '" + request.getEmail() + "' is already in use");
        }

        RegistrationOtpEntry otpEntry = registrationOtpCache.getIfPresent(email);
        if (otpEntry == null) {
            throw new BadRequestException("No active registration verification code found for this email. Please request a new code.");
        }

        int maxAttempts = appProperty.getOtp().getMaxRequests() > 0
                ? appProperty.getOtp().getMaxRequests()
                : 5;

        if (otpEntry.attempts >= maxAttempts) {
            registrationOtpCache.invalidate(email);
            throw new BadRequestException("Too many invalid verification attempts. This code is invalidated. Please request a new code.");
        }

        // Compare submitted OTP against the in-memory cached code
        if (!otpEntry.otp.equals(otp)) {
            otpEntry.attempts++;
            if (otpEntry.attempts >= maxAttempts) {
                registrationOtpCache.invalidate(email);
                throw new BadRequestException("Too many invalid verification attempts. This code is invalidated. Please request a new code.");
            }
            int remaining = maxAttempts - otpEntry.attempts;
            throw new BadRequestException("Invalid verification code. " + remaining + " attempts remaining.");
        }

        // Successfully verified -> invalidate from cache immediately to prevent replay
        registrationOtpCache.invalidate(email);

        // Self-registered users set their own password: mustChangePassword is false
        AppUser user = AppUser.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phone(request.getPhone() != null && !request.getPhone().trim().isEmpty() ? request.getPhone().trim() : null)
                .role(Role.STAFF)
                .active(true)
                .mustChangePassword(false)
                .build();

        AppUser savedUser = userRepository.save(user);
        log.info("Self-registered new user: id={}, username={}, role={}", savedUser.getId(), savedUser.getUsername(), savedUser.getRole());

        // Auto-login: issue JWT token immediately
        UserDetails userDetails = appUserService.loadUserByUsername(savedUser.getUsername());
        String token = jwtService.generateToken(userDetails, savedUser.getRole().name());
        long expiresIn = appProperty.getJwt().getAccessExpiration() / 1000;

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .unit("seconds")
                .username(savedUser.getUsername())
                .role(savedUser.getRole().name())
                .mustChangePassword(false)
                .build();
    }

    /**
     * Visible for testing to verify cached registration OTP without querying database.
     */
    public String getRegistrationOtpForTesting(String email) {
        if (email == null) return null;
        RegistrationOtpEntry entry = registrationOtpCache.getIfPresent(email.trim().toLowerCase());
        return entry != null ? entry.otp : null;
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
            log.warn("Password reset requested for ADMIN account ({}). Admin self-service reset is not allowed.", user.getUsername());
            throw new BadRequestException("Administrator accounts cannot be reset via forgot password.");
        }

        if (!user.isActive()) {
            log.warn("Password reset requested for disabled account: {}", user.getUsername());
            return;
        }

        // Generate 6-digit numeric OTP
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));

        // Invalidate any previous active OTPs for this account
        userTokenRepository.revokeAllActiveTokensByUserId(user.getId());

        int pendingExpirationSeconds = appProperty.getOtp().getPendingExpiration() > 0
                ? appProperty.getOtp().getPendingExpiration()
                : 300;

        AppUserToken tokenEntity = AppUserToken.builder()
                .user(user)
                .token(otp)
                .expiresAt(Instant.now().plus(Duration.ofSeconds(pendingExpirationSeconds)))
                .revoked(false)
                .attempts(0)
                .build();
        userTokenRepository.save(tokenEntity);

        emailService.sendPasswordResetEmail(user.getEmail(), otp);
        log.info("Issued password reset OTP for user: id={}, email={}", user.getId(), user.getEmail());
    }

    @Override
    @Transactional(noRollbackFor = BadRequestException.class)
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String otp = request.getOtp().trim();

        // 1. Account binding: Safely find the targeted account
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token / OTP code"));

        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Administrator accounts cannot be reset via self-service.");
        }

        // 2. Lock active tokens for this user to prevent concurrent bypass of the attempt limit
        List<AppUserToken> activeTokens = userTokenRepository.findActiveTokensByUserIdForUpdate(user.getId());
        if (activeTokens.isEmpty()) {
            throw new BadRequestException("Invalid or expired password reset token / OTP code");
        }

        AppUserToken tokenEntity = activeTokens.get(0);

        // 3. Expiration verification
        if (tokenEntity.getExpiresAt().isBefore(Instant.now())) {
            tokenEntity.setRevoked(true);
            userTokenRepository.save(tokenEntity);
            throw new BadRequestException("Password reset token has expired. Please request a new one.");
        }

        int maxAttempts = 3;

        // 4. Check if token already exceeded allowed attempts
        if (tokenEntity.getAttempts() >= maxAttempts) {
            tokenEntity.setRevoked(true);
            userTokenRepository.save(tokenEntity);
            throw new BadRequestException("Password reset token has been invalidated due to too many failed attempts. Please request a new one.");
        }

        // 5. Verify OTP against the account's active token
        if (!tokenEntity.getToken().equals(otp)) {
            tokenEntity.setAttempts(tokenEntity.getAttempts() + 1);
            if (tokenEntity.getAttempts() >= maxAttempts) {
                tokenEntity.setRevoked(true);
                userTokenRepository.save(tokenEntity);
                throw new BadRequestException("Password reset token has been invalidated due to too many failed attempts. Please request a new one.");
            }
            userTokenRepository.save(tokenEntity);
            throw new BadRequestException("Invalid or expired password reset token / OTP code");
        }

        // 6. Success: update password and revoke token immediately
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        tokenEntity.setRevoked(true);
        userTokenRepository.save(tokenEntity);

        // Invalidate any other residual active tokens
        for (int i = 1; i < activeTokens.size(); i++) {
            AppUserToken residual = activeTokens.get(i);
            residual.setRevoked(true);
            userTokenRepository.save(residual);
        }

        log.info("Password successfully reset for user id={}, username={}", user.getId(), user.getUsername());
    }

    @Override
    @Transactional
    public void changePassword(com.api.v1.inventory.model.request.ChangePasswordRequest request) {
        AppUser currentUser = currentUserService.getCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new BadRequestException("Current password does not match");
        }

        if (passwordEncoder.matches(request.getNewPassword(), currentUser.getPassword())) {
            throw new BadRequestException("New password cannot be the same as the current password");
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        currentUser.setMustChangePassword(false);
        userRepository.save(currentUser);

        log.info("Password successfully changed by authenticated user: id={}, username={}", currentUser.getId(), currentUser.getUsername());
    }
}
