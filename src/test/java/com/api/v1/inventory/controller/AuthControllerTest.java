package com.api.v1.inventory.controller;

import com.api.v1.inventory.model.entity.AppUser;
import com.api.v1.inventory.model.request.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/auth/login - Success with valid admin credentials")
    void testLoginSuccessAdmin() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("admin")
                .password("Admin@12345")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Login successful")))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.username", is("admin")))
                .andExpect(jsonPath("$.data.role", is("ADMIN")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Success with valid staff credentials")
    void testLoginSuccessStaff() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("staff")
                .password("Staff@12345")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.role", is("STAFF")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Failure with wrong password")
    void testLoginFailureWrongPassword() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("admin")
                .password("WrongPassword999")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Incorrect password")))
                .andExpect(jsonPath("$.errors[0].field", is("password")))
                .andExpect(jsonPath("$.errors[0].message", is("Incorrect password")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Failure with nonexistent username")
    void testLoginFailureNonexistentUser() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("nonexistent_user_999")
                .password("Password@123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Username does not exist")))
                .andExpect(jsonPath("$.errors[0].field", is("username")))
                .andExpect(jsonPath("$.errors[0].message", is("Username does not exist")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Failure with blank fields")
    void testLoginFailureBlankFields() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("")
                .password("")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Success when authenticated with Bearer token")
    void testGetCurrentUserSuccess() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("admin")
                .password("Admin@12345")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseJson).path("data").path("token").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is("admin")))
                .andExpect(jsonPath("$.data.role", is("ADMIN")));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Failure when no token provided")
    void testGetCurrentUserUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Autowired
    private com.api.v1.inventory.repository.AppUserTokenRepository appUserTokenRepository;

    @Autowired
    private com.api.v1.inventory.service.impl.AuthServiceImpl authServiceImpl;

    @Autowired
    private com.api.v1.inventory.repository.AppUserRepository appUserRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private com.api.v1.inventory.config.AppProperty appProperty;

    @Test
    @DisplayName("GET /api/v1/auth/me - Failure when invalid token provided")
    void testGetCurrentUserInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password - Success for staff email and OTP generation")
    void testForgotPasswordStaffSuccess() throws Exception {
        var staffUser = appUserRepository.findByUsernameIgnoreCase("staff").orElseThrow();
        appUserTokenRepository.deleteByUserId(staffUser.getId());

        com.api.v1.inventory.model.request.ForgotPasswordRequest request =
                com.api.v1.inventory.model.request.ForgotPasswordRequest.builder()
                        .email(staffUser.getEmail())
                        .build();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("instructions have been sent")));

        var tokens = appUserTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(staffUser.getId()) && !t.isRevoked())
                .toList();

        org.junit.jupiter.api.Assertions.assertFalse(tokens.isEmpty(), "An active OTP token should be generated for staff");
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password - Disallowed for admin email")
    void testForgotPasswordAdminRestricted() throws Exception {
        var adminUser = appUserRepository.findByUsernameIgnoreCase("admin").orElseThrow();

        com.api.v1.inventory.model.request.ForgotPasswordRequest request =
                com.api.v1.inventory.model.request.ForgotPasswordRequest.builder()
                        .email(adminUser.getEmail())
                        .build();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Administrator accounts cannot be reset")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password - Success resets password and allows login")
    void testResetPasswordStaffSuccess() throws Exception {
        var staffUser = appUserRepository.findByUsernameIgnoreCase("staff").orElseThrow();
        appUserTokenRepository.deleteByUserId(staffUser.getId());

        var tokenEntity = appUserTokenRepository.save(com.api.v1.inventory.model.entity.AppUserToken.builder()
                .user(staffUser)
                .token("998877")
                .expiresAt(java.time.Instant.now().plus(java.time.Duration.ofMinutes(15)))
                .revoked(false)
                .build());

        com.api.v1.inventory.model.request.ResetPasswordRequest resetReq =
                com.api.v1.inventory.model.request.ResetPasswordRequest.builder()
                        .email(staffUser.getEmail())
                        .otp("998877")
                        .newPassword("NewStaffPass@123")
                        .build();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Password reset successfully")));

        // Verify login works with new password
        LoginRequest loginRequest = LoginRequest.builder()
                .username("staff")
                .password("NewStaffPass@123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Restore password for other tests
        staffUser = appUserRepository.findByUsernameIgnoreCase("staff").orElseThrow();
        staffUser.setPassword(passwordEncoder.encode("Staff@12345"));
        appUserRepository.save(staffUser);
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password - Invalid or revoked token rejected")
    void testResetPasswordInvalidTokenRejected() throws Exception {
        var staffUser = appUserRepository.findByUsernameIgnoreCase("staff").orElseThrow();
        com.api.v1.inventory.model.request.ResetPasswordRequest resetReq =
                com.api.v1.inventory.model.request.ResetPasswordRequest.builder()
                        .email(staffUser.getEmail())
                        .otp("000000")
                        .newPassword("AnotherPassword@123")
                        .build();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid or expired")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password - OTP belonging to another account is rejected")
    void testResetPasswordRejectsOtpFromAnotherAccount() throws Exception {
        // Account A: staff
        var staffUser = appUserRepository.findByUsernameIgnoreCase("staff").orElseThrow();
        appUserTokenRepository.deleteByUserId(staffUser.getId());
        appUserTokenRepository.save(com.api.v1.inventory.model.entity.AppUserToken.builder()
                .user(staffUser)
                .token("112233")
                .expiresAt(java.time.Instant.now().plus(java.time.Duration.ofMinutes(15)))
                .revoked(false)
                .build());

        // Account B: create second staff user
        String userBName = "staff_b_" + System.currentTimeMillis();
        var userB = appUserRepository.save(com.api.v1.inventory.model.entity.AppUser.builder()
                .username(userBName)
                .email(userBName + "@test.com")
                .password(passwordEncoder.encode("PasswordB@123"))
                .fullName("Staff B")
                .role(com.api.v1.inventory.model.enumerate.Role.STAFF)
                .active(true)
                .build());

        appUserTokenRepository.save(com.api.v1.inventory.model.entity.AppUserToken.builder()
                .user(userB)
                .token("889900")
                .expiresAt(java.time.Instant.now().plus(java.time.Duration.ofMinutes(15)))
                .revoked(false)
                .build());

        // Attempt to reset Account B using Account A's OTP ("112233")
        com.api.v1.inventory.model.request.ResetPasswordRequest resetReq =
                com.api.v1.inventory.model.request.ResetPasswordRequest.builder()
                        .email(userB.getEmail())
                        .otp("112233")
                        .newPassword("HackedPassword@123")
                        .build();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid or expired")));

        // Verify Account B's password has NOT changed
        var refreshedB = appUserRepository.findById(userB.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("PasswordB@123", refreshedB.getPassword()));
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password - Expired OTP is rejected")
    void testResetPasswordExpiredOtpRejected() throws Exception {
        var staffUser = appUserRepository.findByUsernameIgnoreCase("staff").orElseThrow();
        appUserTokenRepository.deleteByUserId(staffUser.getId());
        appUserTokenRepository.save(com.api.v1.inventory.model.entity.AppUserToken.builder()
                .user(staffUser)
                .token("445566")
                .expiresAt(java.time.Instant.now().minusSeconds(10))
                .revoked(false)
                .build());

        com.api.v1.inventory.model.request.ResetPasswordRequest resetReq =
                com.api.v1.inventory.model.request.ResetPasswordRequest.builder()
                        .email(staffUser.getEmail())
                        .otp("445566")
                        .newPassword("NewStaffPass@123")
                        .build();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("expired")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password - Successfully used OTP cannot be reused")
    void testResetPasswordUsedOtpCannotBeReused() throws Exception {
        var staffUser = appUserRepository.findByUsernameIgnoreCase("staff").orElseThrow();
        appUserTokenRepository.deleteByUserId(staffUser.getId());
        appUserTokenRepository.save(com.api.v1.inventory.model.entity.AppUserToken.builder()
                .user(staffUser)
                .token("778899")
                .expiresAt(java.time.Instant.now().plus(java.time.Duration.ofMinutes(15)))
                .revoked(false)
                .build());

        com.api.v1.inventory.model.request.ResetPasswordRequest resetReq =
                com.api.v1.inventory.model.request.ResetPasswordRequest.builder()
                        .email(staffUser.getEmail())
                        .otp("778899")
                        .newPassword("FirstResetPass@123")
                        .build();

        // First use succeeds
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Second use with identical OTP is rejected
        com.api.v1.inventory.model.request.ResetPasswordRequest reuseReq =
                com.api.v1.inventory.model.request.ResetPasswordRequest.builder()
                        .email(staffUser.getEmail())
                        .otp("778899")
                        .newPassword("SecondResetPass@123")
                        .build();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reuseReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid or expired")));

        // Restore password
        staffUser = appUserRepository.findByUsernameIgnoreCase("staff").orElseThrow();
        staffUser.setPassword(passwordEncoder.encode("Staff@12345"));
        appUserRepository.save(staffUser);
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password - Three failed attempts invalidate the OTP")
    void testResetPasswordThreeFailedAttemptsInvalidatesOtp() throws Exception {
        var staffUser = appUserRepository.findByUsernameIgnoreCase("staff").orElseThrow();
        appUserTokenRepository.deleteByUserId(staffUser.getId());
        appUserTokenRepository.save(com.api.v1.inventory.model.entity.AppUserToken.builder()
                .user(staffUser)
                .token("123987")
                .expiresAt(java.time.Instant.now().plus(java.time.Duration.ofMinutes(15)))
                .revoked(false)
                .attempts(0)
                .build());

        // Attempt 1: wrong OTP
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(com.api.v1.inventory.model.request.ResetPasswordRequest.builder()
                                .email(staffUser.getEmail())
                                .otp("000001")
                                .newPassword("NewPass@123")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid or expired")));

        // Attempt 2: wrong OTP
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(com.api.v1.inventory.model.request.ResetPasswordRequest.builder()
                                .email(staffUser.getEmail())
                                .otp("000002")
                                .newPassword("NewPass@123")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid or expired")));

        // Attempt 3: wrong OTP -> limit reached, token revoked
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(com.api.v1.inventory.model.request.ResetPasswordRequest.builder()
                                .email(staffUser.getEmail())
                                .otp("000003")
                                .newPassword("NewPass@123")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("too many failed attempts")));

        // Attempt 4: submit the genuine OTP -> still rejected because token was invalidated
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(com.api.v1.inventory.model.request.ResetPasswordRequest.builder()
                                .email(staffUser.getEmail())
                                .otp("123987")
                                .newPassword("NewPass@123")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid or expired")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password - New OTP request invalidates previous active OTP")
    void testNewOtpRequestInvalidatesPreviousActiveOtp() throws Exception {
        var staffUser = appUserRepository.findByUsernameIgnoreCase("staff").orElseThrow();
        appUserTokenRepository.deleteByUserId(staffUser.getId());

        com.api.v1.inventory.model.request.ForgotPasswordRequest request =
                com.api.v1.inventory.model.request.ForgotPasswordRequest.builder()
                        .email(staffUser.getEmail())
                        .build();

        // 1st request
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        var firstToken = appUserTokenRepository.findByUserIdAndRevokedFalse(staffUser.getId()).get(0);
        String firstOtp = firstToken.getToken();

        // 2nd request
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify first token is marked revoked
        var refreshedFirst = appUserTokenRepository.findById(firstToken.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(refreshedFirst.isRevoked(), "First OTP must be marked revoked");

        // Try to use 1st OTP -> rejected
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(com.api.v1.inventory.model.request.ResetPasswordRequest.builder()
                                .email(staffUser.getEmail())
                                .otp(firstOtp)
                                .newPassword("SomeNewPass@123")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid or expired")));

        // Use 2nd OTP -> succeeds
        var secondToken = appUserTokenRepository.findByUserIdAndRevokedFalse(staffUser.getId()).get(0);
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(com.api.v1.inventory.model.request.ResetPasswordRequest.builder()
                                .email(staffUser.getEmail())
                                .otp(secondToken.getToken())
                                .newPassword("SomeNewPass@123")
                                .build())))
                .andExpect(status().isOk());

        // Restore password
        staffUser = appUserRepository.findByUsernameIgnoreCase("staff").orElseThrow();
        staffUser.setPassword(passwordEncoder.encode("Staff@12345"));
        appUserRepository.save(staffUser);
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password - Rejects request with missing email")
    void testResetPasswordRejectsMissingEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"123456\",\"newPassword\":\"ValidPass@123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Password reset OTP value does NOT appear in application logs")
    void testOtpValueNotInApplicationLogs() throws Exception {
        Logger emailLogger = (Logger) LoggerFactory.getLogger(com.api.v1.inventory.service.impl.EmailServiceImpl.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        emailLogger.addAppender(listAppender);

        try {
            var staffUser = appUserRepository.findByUsernameIgnoreCase("staff").orElseThrow();
            appUserTokenRepository.deleteByUserId(staffUser.getId());

            com.api.v1.inventory.model.request.ForgotPasswordRequest request =
                    com.api.v1.inventory.model.request.ForgotPasswordRequest.builder()
                            .email(staffUser.getEmail())
                            .build();

            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // Retrieve generated OTP from database
            var tokenEntity = appUserTokenRepository.findByUserIdAndRevokedFalse(staffUser.getId()).get(0);
            String rawOtp = tokenEntity.getToken();

            // Verify the raw OTP does not appear in any log event
            boolean leaked = listAppender.list.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains(rawOtp));

            org.junit.jupiter.api.Assertions.assertFalse(leaked, "Active OTP value must never be logged in application logs");
        } finally {
            emailLogger.detachAppender(listAppender);
        }
    }

    @Test
    @DisplayName("POST /api/v1/auth/change-password - Success changes password for authenticated user")
    void testChangePasswordSuccess() throws Exception {
        LoginRequest loginStaffReq = LoginRequest.builder()
                .username("staff")
                .password("Staff@12345")
                .build();

        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginStaffReq)))
                .andExpect(status().isOk())
                .andReturn();

        String staffToken = objectMapper.readTree(loginRes.getResponse().getContentAsString())
                .path("data").path("token").asText();

        com.api.v1.inventory.model.request.ChangePasswordRequest changeReq =
                com.api.v1.inventory.model.request.ChangePasswordRequest.builder()
                        .currentPassword("Staff@12345")
                        .newPassword("NewStaffPass@123")
                        .build();

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Password changed successfully")));

        // Verify login works with the updated password
        LoginRequest loginRequest = LoginRequest.builder()
                .username("staff")
                .password("NewStaffPass@123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Restore original password for other tests
        var staffUser = appUserRepository.findByUsernameIgnoreCase("staff").orElseThrow();
        staffUser.setPassword(passwordEncoder.encode("Staff@12345"));
        appUserRepository.save(staffUser);
    }

    @Test
    @DisplayName("POST /api/v1/auth/change-password - Fails when current password does not match")
    void testChangePasswordWrongCurrentPassword() throws Exception {
        LoginRequest loginStaffReq = LoginRequest.builder()
                .username("staff")
                .password("Staff@12345")
                .build();

        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginStaffReq)))
                .andExpect(status().isOk())
                .andReturn();

        String staffToken = objectMapper.readTree(loginRes.getResponse().getContentAsString())
                .path("data").path("token").asText();

        com.api.v1.inventory.model.request.ChangePasswordRequest changeReq =
                com.api.v1.inventory.model.request.ChangePasswordRequest.builder()
                        .currentPassword("WrongOldPass@999")
                        .newPassword("NewStaffPass@123")
                        .build();

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Current password does not match")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/change-password - Fails when unauthenticated")
    void testChangePasswordUnauthenticated() throws Exception {
        com.api.v1.inventory.model.request.ChangePasswordRequest changeReq =
                com.api.v1.inventory.model.request.ChangePasswordRequest.builder()
                        .currentPassword("Staff@12345")
                        .newPassword("NewStaffPass@123")
                        .build();

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password - Rejects non-OTP JWT tokens")
    void testResetPasswordRejectsJwtTokens() throws Exception {
        var staffUser = appUserRepository.findByUsernameIgnoreCase("staff").orElseThrow();
        com.api.v1.inventory.model.request.ResetPasswordRequest resetReq =
                com.api.v1.inventory.model.request.ResetPasswordRequest.builder()
                        .email(staffUser.getEmail())
                        .otp("eyJhbGciOiJIUzI1NiJ9.fakeToken")
                        .newPassword("SomePassword@123")
                        .build();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid or expired")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Failure when user account is inactive/disabled")
    void testLoginFailureInactiveUser() throws Exception {
        String uniqueUsername = "dis_user_" + System.currentTimeMillis();
        var disabledUser = appUserRepository.save(com.api.v1.inventory.model.entity.AppUser.builder()
                .username(uniqueUsername)
                .email(uniqueUsername + "@test.com")
                .password(passwordEncoder.encode("Password@123"))
                .fullName("Disabled User")
                .role(com.api.v1.inventory.model.enumerate.Role.STAFF)
                .active(false)
                .build());

        LoginRequest request = LoginRequest.builder()
                .username(uniqueUsername)
                .password("Password@123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("disabled")));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Deactivated user token is rejected with 401 Unauthorized")
    void testDeactivatedUserTokenRejected() throws Exception {
        String uniqueUsername = "deact_user_" + System.currentTimeMillis();
        var activeUser = appUserRepository.save(com.api.v1.inventory.model.entity.AppUser.builder()
                .username(uniqueUsername)
                .email(uniqueUsername + "@test.com")
                .password(passwordEncoder.encode("Password@123"))
                .fullName("Active Then Deactivated")
                .role(com.api.v1.inventory.model.enumerate.Role.STAFF)
                .active(true)
                .build());

        LoginRequest loginRequest = LoginRequest.builder()
                .username(uniqueUsername)
                .password("Password@123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("token").asText();

        // Now deactivate user
        activeUser.setActive(false);
        appUserRepository.save(activeUser);

        // Attempt to call protected endpoint with previously issued token
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/auth/send-registration-otp - Success sends OTP for valid Gmail")
    void testSendRegistrationOtpSuccess() throws Exception {
        String testEmail = "testreg" + System.currentTimeMillis() + "@gmail.com";
        var request = com.api.v1.inventory.model.request.SendRegistrationOtpRequest.builder()
                .email(testEmail)
                .build();

        mockMvc.perform(post("/api/v1/auth/send-registration-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Verification code (OTP) has been sent")));

        String cachedOtp = authServiceImpl.getRegistrationOtpForTesting(testEmail);
        org.junit.jupiter.api.Assertions.assertNotNull(cachedOtp, "Registration OTP must be cached in memory");
        org.junit.jupiter.api.Assertions.assertEquals(6, cachedOtp.length());
    }

    @Test
    @DisplayName("POST /api/v1/auth/send-registration-otp - Reject non-Gmail address")
    void testSendRegistrationOtpRejectNonGmail() throws Exception {
        var request = com.api.v1.inventory.model.request.SendRegistrationOtpRequest.builder()
                .email("user@yahoo.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/send-registration-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Success creates user and auto-logins with JWT token")
    void testRegisterSuccessWithAutoLogin() throws Exception {
        long ts = System.currentTimeMillis();
        String testEmail = "selfreg" + ts + "@gmail.com";
        String username = "selfuser_" + ts;

        // 1. Generate OTP
        mockMvc.perform(post("/api/v1/auth/send-registration-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                com.api.v1.inventory.model.request.SendRegistrationOtpRequest.builder()
                                        .email(testEmail)
                                        .build())))
                .andExpect(status().isOk());

        String generatedOtp = authServiceImpl.getRegistrationOtpForTesting(testEmail);
        org.junit.jupiter.api.Assertions.assertNotNull(generatedOtp, "Generated OTP must be in cache");

        // 2. Register
        var registerRequest = com.api.v1.inventory.model.request.RegisterRequest.builder()
                .username(username)
                .email(testEmail)
                .password("Password@123")
                .fullName("Self Registered Staff")
                .phone("+85512345678")
                .otp(generatedOtp)
                .build();

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.username", is(username)))
                .andExpect(jsonPath("$.data.role", is("STAFF")))
                .andExpect(jsonPath("$.data.mustChangePassword", is(false)))
                .andReturn();

        // 3. Verify returned JWT token immediately works on protected endpoints
        String jwtToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .path("data").path("token").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username", is(username)));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Rejects invalid OTP")
    void testRegisterInvalidOtpRejected() throws Exception {
        long ts = System.currentTimeMillis();
        String testEmail = "wrongotp" + ts + "@gmail.com";

        mockMvc.perform(post("/api/v1/auth/send-registration-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                com.api.v1.inventory.model.request.SendRegistrationOtpRequest.builder()
                                        .email(testEmail)
                                        .build())))
                .andExpect(status().isOk());

        var registerRequest = com.api.v1.inventory.model.request.RegisterRequest.builder()
                .username("wrong_user_" + ts)
                .email(testEmail)
                .password("Password@123")
                .fullName("Wrong OTP User")
                .otp("999999")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid verification code")));
    }

    @Test
    @DisplayName("Admin-created user requires password change on first login")
    void testAdminCreatedUserRequiresPasswordChangeOnFirstLogin() throws Exception {
        long ts = System.currentTimeMillis();
        String username = "admin_created_" + ts;

        // Admin creates user via entity with mustChangePassword = true (matching AppUserServiceImpl.createUser)
        var user = appUserRepository.save(com.api.v1.inventory.model.entity.AppUser.builder()
                .username(username)
                .email(username + "@gmail.com")
                .password(passwordEncoder.encode("TempAdminPass@123"))
                .fullName("Admin Created Staff")
                .role(com.api.v1.inventory.model.enumerate.Role.STAFF)
                .active(true)
                .mustChangePassword(true)
                .build());

        // First login -> mustChangePassword is TRUE
        LoginRequest loginRequest = LoginRequest.builder()
                .username(username)
                .password("TempAdminPass@123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mustChangePassword", is(true)))
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("token").asText();

        // User changes password
        var changeReq = com.api.v1.inventory.model.request.ChangePasswordRequest.builder()
                .currentPassword("TempAdminPass@123")
                .newPassword("MyOwnSecurePass@123")
                .build();

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isOk());

        // Subsequent login -> mustChangePassword is FALSE
        LoginRequest secondLogin = LoginRequest.builder()
                .username(username)
                .password("MyOwnSecurePass@123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mustChangePassword", is(false)));
    }

    @Test
    @DisplayName("OTP expiration configuration - OTP expires after 50 seconds")
    void testOtpExpiresAfter50Seconds() throws Exception {
        org.junit.jupiter.api.Assertions.assertEquals(50, appProperty.getOtp().getPendingExpiration());

        AppUser staffUser = appUserRepository.findByUsernameIgnoreCase("staff").orElseThrow();
        authServiceImpl.forgotPassword(com.api.v1.inventory.model.request.ForgotPasswordRequest.builder()
                .email(staffUser.getEmail())
                .build());

        var tokens = appUserTokenRepository.findByUserIdAndRevokedFalse(staffUser.getId());
        org.junit.jupiter.api.Assertions.assertFalse(tokens.isEmpty());
        var token = tokens.get(0);

        long diffSeconds = java.time.Duration.between(java.time.Instant.now(), token.getExpiresAt()).getSeconds();
        org.junit.jupiter.api.Assertions.assertTrue(diffSeconds >= 45 && diffSeconds <= 50,
                "OTP expiration should be ~50 seconds in the future, got: " + diffSeconds);
    }
}
