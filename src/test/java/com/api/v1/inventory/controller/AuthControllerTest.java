package com.api.v1.inventory.controller;

import com.api.v1.inventory.model.request.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
    private com.api.v1.inventory.repository.AppUserRepository appUserRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

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
        com.api.v1.inventory.model.request.ResetPasswordRequest resetReq =
                com.api.v1.inventory.model.request.ResetPasswordRequest.builder()
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
        com.api.v1.inventory.model.request.ResetPasswordRequest resetReq =
                com.api.v1.inventory.model.request.ResetPasswordRequest.builder()
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
}
