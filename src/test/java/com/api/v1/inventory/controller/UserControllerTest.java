package com.api.v1.inventory.controller;

import com.api.v1.inventory.model.enumerate.Role;
import com.api.v1.inventory.model.request.CreateUserRequest;
import com.api.v1.inventory.model.request.LoginRequest;
import com.api.v1.inventory.model.request.UpdateUserRequest;
import com.api.v1.inventory.model.request.UpdateUserStatusRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String staffToken;

    @BeforeEach
    void setupTokens() throws Exception {
        adminToken = obtainToken("admin", "Admin@12345");
        staffToken = obtainToken("staff", "Staff@12345");
    }

    private String obtainToken(String username, String password) throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username(username)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    @Test
    @DisplayName("POST /api/v1/users - Admin successfully creates a new user")
    void testAdminCreatesUser() throws Exception {
        String uniqueUser = "testuser_" + System.currentTimeMillis();
        CreateUserRequest request = CreateUserRequest.builder()
                .username(uniqueUser)
                .email(uniqueUser + "@test.com")
                .password("Password@123")
                .fullName("Test User Full")
                .phone("098765432")
                .role(Role.STAFF)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is(uniqueUser)))
                .andExpect(jsonPath("$.data.role", is("STAFF")))
                .andExpect(jsonPath("$.data.active", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/users - Reject duplicate username")
    void testCreateDuplicateUsernameRejected() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("admin") // already exists
                .email("admin_new@test.com")
                .password("Password@123")
                .fullName("Duplicate Admin")
                .role(Role.ADMIN)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already taken")));
    }

    @Test
    @DisplayName("POST /api/v1/users - Staff role is Forbidden (403)")
    void testStaffCannotCreateUser() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("unauthorized_user")
                .password("Password@123")
                .fullName("Unauthorized")
                .role(Role.STAFF)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/users - Staff role is Forbidden (403)")
    void testStaffCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/users - Unauthenticated access is Unauthorized (401)")
    void testUnauthenticatedCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/users - Admin can list users with pagination")
    void testAdminCanListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users?page=0&size=5")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()))
                .andExpect(jsonPath("$.data.pageSize", is(5)));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - Admin updates user successfully")
    void testAdminUpdatesUser() throws Exception {
        String uniqueUser = "updatable_" + System.currentTimeMillis();
        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username(uniqueUser)
                .email(uniqueUser + "@test.com")
                .password("Password@123")
                .fullName("Before Update")
                .phone("011223344")
                .role(Role.STAFF)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        long userId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .fullName("After Update Name")
                .email(uniqueUser + "_updated@test.com")
                .phone("099887766")
                .role(Role.STAFF)
                .build();

        mockMvc.perform(put("/api/v1/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fullName", is("After Update Name")))
                .andExpect(jsonPath("$.data.phone", is("099887766")));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id}/status - Admin disables and re-enables user")
    void testAdminTogglesUserStatus() throws Exception {
        String uniqueUser = "status_test_" + System.currentTimeMillis();
        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username(uniqueUser)
                .password("Password@123")
                .fullName("Status User")
                .role(Role.STAFF)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        long userId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Deactivate user
        mockMvc.perform(patch("/api/v1/users/" + userId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserStatusRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active", is(false)));

        // Re-enable user
        mockMvc.perform(patch("/api/v1/users/" + userId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserStatusRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active", is(true)));
    }
}
