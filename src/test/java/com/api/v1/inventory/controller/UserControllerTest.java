package com.api.v1.inventory.controller;

import com.api.v1.inventory.model.entity.AppUser;
import com.api.v1.inventory.model.entity.Category;
import com.api.v1.inventory.model.entity.Product;
import com.api.v1.inventory.model.entity.StockTransaction;
import com.api.v1.inventory.model.enumerate.Role;
import com.api.v1.inventory.model.enumerate.TransactionType;
import com.api.v1.inventory.model.request.CreateUserRequest;
import com.api.v1.inventory.model.request.LoginRequest;
import com.api.v1.inventory.model.request.UpdateUserRequest;
import com.api.v1.inventory.model.request.UpdateUserStatusRequest;
import com.api.v1.inventory.repository.AppUserRepository;
import com.api.v1.inventory.repository.CategoryRepository;
import com.api.v1.inventory.repository.ProductRepository;
import com.api.v1.inventory.repository.StockTransactionRepository;
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

import java.math.BigDecimal;

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

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private String adminToken;
    private String staffToken;
    private String managerToken;

    @BeforeEach
    void setupTokens() throws Exception {
        adminToken = obtainToken("admin", "Admin@12345");
        staffToken = obtainToken("staff", "Staff@12345");
        managerToken = obtainToken("manager", "Manager@12345");
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
                .email(uniqueUser + "@gmail.com")
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
                .email("admin_new@gmail.com")
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
    @DisplayName("POST /api/v1/users - Manager successfully creates a STAFF user")
    void testManagerCreatesStaffUser() throws Exception {
        String uniqueUser = "mgr_staff_" + System.currentTimeMillis();
        CreateUserRequest request = CreateUserRequest.builder()
                .username(uniqueUser)
                .email(uniqueUser + "@gmail.com")
                .password("Password@123")
                .fullName("Staff created by Manager")
                .phone("011223344")
                .role(Role.STAFF)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is(uniqueUser)))
                .andExpect(jsonPath("$.data.role", is("STAFF")));
    }

    @Test
    @DisplayName("POST /api/v1/users - Staff role cannot create Admin (403)")
    void testStaffCannotCreateAdmin() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("unauthorized_admin")
                .password("Password@123")
                .fullName("Unauthorized Admin")
                .role(Role.ADMIN)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/users - Management cannot create MANAGER role (400)")
    void testManagerCannotCreateManager() throws Exception {
        String uniqueUser = "mgr_mgr_" + System.currentTimeMillis();
        CreateUserRequest request = CreateUserRequest.builder()
                .username(uniqueUser)
                .password("Password@123")
                .fullName("Manager creating Manager")
                .role(Role.MANAGER)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Management role is only permitted to create Staff and Admin accounts")));
    }

    @Test
    @DisplayName("POST /api/v1/users - Enforce max 3 Admins limit for both Admin and Management")
    void testMaxAdminLimitEnforced() throws Exception {
        long initialAdminCount = userRepository.countByRole(Role.ADMIN);
        java.util.List<Long> createdAdminIds = new java.util.ArrayList<>();

        try {
            int toCreate = (int) (3 - initialAdminCount);
            for (int i = 0; i < toCreate; i++) {
                String adminUsername = "extra_admin_" + i + "_" + System.currentTimeMillis();
                CreateUserRequest createReq = CreateUserRequest.builder()
                        .username(adminUsername)
                        .email(adminUsername + "@gmail.com")
                        .password("AdminPass@123")
                        .fullName("Extra Admin " + i)
                        .role(Role.ADMIN)
                        .build();

                String token = (i % 2 == 0) ? managerToken : adminToken;

                MvcResult res = mockMvc.perform(post("/api/v1/users")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createReq)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.role", is("ADMIN")))
                        .andReturn();

                long id = objectMapper.readTree(res.getResponse().getContentAsString())
                        .path("data").path("id").asLong();
                createdAdminIds.add(id);
            }

            org.junit.jupiter.api.Assertions.assertEquals(3, userRepository.countByRole(Role.ADMIN));

            String fourthAdminAdmin = "fourth_admin_a_" + System.currentTimeMillis();
            CreateUserRequest fourthByAdminReq = CreateUserRequest.builder()
                    .username(fourthAdminAdmin)
                    .password("AdminPass@123")
                    .fullName("Fourth Admin By Admin")
                    .role(Role.ADMIN)
                    .build();

            mockMvc.perform(post("/api/v1/users")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(fourthByAdminReq)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("Maximum limit of 3 administrator accounts has been reached")));

            String fourthAdminMgr = "fourth_admin_m_" + System.currentTimeMillis();
            CreateUserRequest fourthByMgrReq = CreateUserRequest.builder()
                    .username(fourthAdminMgr)
                    .password("AdminPass@123")
                    .fullName("Fourth Admin By Manager")
                    .role(Role.ADMIN)
                    .build();

            mockMvc.perform(post("/api/v1/users")
                            .header("Authorization", "Bearer " + managerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(fourthByMgrReq)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("Maximum limit of 3 administrator accounts has been reached")));

        } finally {
            for (Long id : createdAdminIds) {
                userRepository.deleteById(id);
            }
        }
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - Cannot promote user to ADMIN when max 3 limit reached")
    void testCannotPromoteToAdminWhenLimitReached() throws Exception {
        long initialAdminCount = userRepository.countByRole(Role.ADMIN);
        java.util.List<Long> createdAdminIds = new java.util.ArrayList<>();
        Long staffUserId = null;

        try {
            String staffUsername = "promotable_staff_" + System.currentTimeMillis();
            AppUser staffUser = userRepository.save(AppUser.builder()
                    .username(staffUsername)
                    .password("Pass@123")
                    .fullName("Staff To Promote")
                    .role(Role.STAFF)
                    .active(true)
                    .build());
            staffUserId = staffUser.getId();

            int toCreate = (int) (3 - initialAdminCount);
            for (int i = 0; i < toCreate; i++) {
                String adminUsername = "fill_admin_" + i + "_" + System.currentTimeMillis();
                AppUser extraAdmin = userRepository.save(AppUser.builder()
                        .username(adminUsername)
                        .password("AdminPass@123")
                        .fullName("Fill Admin " + i)
                        .role(Role.ADMIN)
                        .active(true)
                        .build());
                createdAdminIds.add(extraAdmin.getId());
            }

            UpdateUserRequest updateReq = UpdateUserRequest.builder()
                    .fullName("Promoted Admin")
                    .role(Role.ADMIN)
                    .build();

            mockMvc.perform(put("/api/v1/users/" + staffUserId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("Maximum limit of 3 administrator accounts has been reached")));

        } finally {
            for (Long id : createdAdminIds) {
                userRepository.deleteById(id);
            }
            if (staffUserId != null) {
                userRepository.deleteById(staffUserId);
            }
        }
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
                .email(uniqueUser + "@gmail.com")
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
                .email(uniqueUser + "_updated@gmail.com")
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
        mockMvc.perform(put("/api/v1/users/" + userId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserStatusRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active", is(false)));

        // Re-enable user
        mockMvc.perform(put("/api/v1/users/" + userId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserStatusRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active", is(true)));
    }

    @Test
    @DisplayName("GET /api/v1/users - Invalid sort field (hackerField) returns 400")
    void testInvalidSortFieldReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/users?sort=hackerField,asc")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid sort property 'hackerField'")));
    }

    @Test
    @DisplayName("GET /api/v1/users - Invalid sort direction (random) returns 400")
    void testInvalidSortDirectionReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/users?sort=id,random")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid sort property 'random'")));
    }

    @Test
    @DisplayName("GET /api/v1/users - Negative page index (-1) returns 400")
    void testNegativePageIndexReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/users?page=-1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Page index must not be less than zero (0-indexed)")));
    }

    @Test
    @DisplayName("GET /api/v1/users - Zero page size (0) returns 400")
    void testZeroPageSizeReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/users?size=0")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Page size must not be less than 1")));
    }

    @Test
    @DisplayName("GET /api/v1/users - Oversized page size (200) is capped at 100")
    void testOversizedPageSizeCappedAt100() throws Exception {
        mockMvc.perform(get("/api/v1/users?size=200")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.pageSize", is(100)));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Admin deletes single user successfully")
    void testAdminDeletesSingleUserSuccessfully() throws Exception {
        long ts = System.currentTimeMillis();
        AppUser user = userRepository.save(AppUser.builder()
                .username("del_single_" + ts)
                .password("Password@123")
                .fullName("Delete Single User")
                .role(Role.STAFF)
                .active(true)
                .build());

        mockMvc.perform(delete("/api/v1/users/" + user.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("User deleted successfully")));

        org.junit.jupiter.api.Assertions.assertFalse(userRepository.existsById(user.getId()));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{ids} - Bulk delete multiple users successfully")
    void testAdminDeletesMultipleUsersSuccessfully() throws Exception {
        long ts = System.currentTimeMillis();
        AppUser u1 = userRepository.save(AppUser.builder().username("bulk_u1_" + ts).password("Pass@123").fullName("Bulk User 1").role(Role.STAFF).build());
        AppUser u2 = userRepository.save(AppUser.builder().username("bulk_u2_" + ts).password("Pass@123").fullName("Bulk User 2").role(Role.STAFF).build());
        AppUser u3 = userRepository.save(AppUser.builder().username("bulk_u3_" + ts).password("Pass@123").fullName("Bulk User 3").role(Role.STAFF).build());

        mockMvc.perform(delete("/api/v1/users/" + u1.getId() + "," + u2.getId() + "," + u3.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Users deleted successfully")));

        org.junit.jupiter.api.Assertions.assertFalse(userRepository.existsById(u1.getId()));
        org.junit.jupiter.api.Assertions.assertFalse(userRepository.existsById(u2.getId()));
        org.junit.jupiter.api.Assertions.assertFalse(userRepository.existsById(u3.getId()));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{ids} - Empty entries like '1,,2' are skipped")
    void testBulkDeleteSkipsEmptyEntries() throws Exception {
        long ts = System.currentTimeMillis();
        AppUser u1 = userRepository.save(AppUser.builder().username("skip_u1_" + ts).password("Pass@123").fullName("Skip User 1").role(Role.STAFF).build());
        AppUser u2 = userRepository.save(AppUser.builder().username("skip_u2_" + ts).password("Pass@123").fullName("Skip User 2").role(Role.STAFF).build());

        mockMvc.perform(delete("/api/v1/users/" + u1.getId() + ",," + u2.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Users deleted successfully")));

        org.junit.jupiter.api.Assertions.assertFalse(userRepository.existsById(u1.getId()));
        org.junit.jupiter.api.Assertions.assertFalse(userRepository.existsById(u2.getId()));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{ids} - Duplicate IDs are handled cleanly")
    void testBulkDeleteHandlesDuplicateIds() throws Exception {
        long ts = System.currentTimeMillis();
        AppUser u1 = userRepository.save(AppUser.builder().username("dup_u1_" + ts).password("Pass@123").fullName("Dup User 1").role(Role.STAFF).build());

        mockMvc.perform(delete("/api/v1/users/" + u1.getId() + "," + u1.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("User deleted successfully")));

        org.junit.jupiter.api.Assertions.assertFalse(userRepository.existsById(u1.getId()));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{ids} - Bulk delete with non-existent ID fails atomically")
    void testBulkDeleteNonExistentUserFailsAll() throws Exception {
        long ts = System.currentTimeMillis();
        AppUser u1 = userRepository.save(AppUser.builder().username("exist_u1_" + ts).password("Pass@123").fullName("Exist User 1").role(Role.STAFF).build());

        mockMvc.perform(delete("/api/v1/users/" + u1.getId() + ",999991,999992")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Users not found with IDs: [999991, 999992]")))
                .andExpect(jsonPath("$.message", containsString("Nothing was deleted")));

        org.junit.jupiter.api.Assertions.assertTrue(userRepository.existsById(u1.getId()), "Existing user must NOT be deleted if batch fails");
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Single non-existent user returns 404")
    void testSingleNonExistentUserReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/users/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("User not found with ID: 999999")))
                .andExpect(jsonPath("$.message", containsString("Nothing was deleted")));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Admin cannot delete own account")
    void testAdminCannotDeleteOwnAccountSingle() throws Exception {
        AppUser adminUser = userRepository.findByUsernameIgnoreCase("admin").orElseThrow();

        mockMvc.perform(delete("/api/v1/users/" + adminUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("You cannot delete your own account")));

        org.junit.jupiter.api.Assertions.assertTrue(userRepository.existsById(adminUser.getId()));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{ids} - Bulk delete including own account fails atomically")
    void testAdminCannotDeleteOwnAccountInBulk() throws Exception {
        long ts = System.currentTimeMillis();
        AppUser u1 = userRepository.save(AppUser.builder().username("self_chk_" + ts).password("Pass@123").fullName("Self Check User").role(Role.STAFF).build());
        AppUser adminUser = userRepository.findByUsernameIgnoreCase("admin").orElseThrow();

        mockMvc.perform(delete("/api/v1/users/" + u1.getId() + "," + adminUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("You cannot delete your own account")))
                .andExpect(jsonPath("$.message", containsString("Nothing was deleted")));

        org.junit.jupiter.api.Assertions.assertTrue(userRepository.existsById(u1.getId()), "User must NOT be deleted");
        org.junit.jupiter.api.Assertions.assertTrue(userRepository.existsById(adminUser.getId()), "Admin must NOT be deleted");
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Staff role cannot delete users (403)")
    void testStaffCannotDeleteUsers() throws Exception {
        long ts = System.currentTimeMillis();
        AppUser u1 = userRepository.save(AppUser.builder().username("staff_no_del_" + ts).password("Pass@123").fullName("No Delete User").role(Role.STAFF).build());

        mockMvc.perform(delete("/api/v1/users/" + u1.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));

        org.junit.jupiter.api.Assertions.assertTrue(userRepository.existsById(u1.getId()));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Unauthenticated delete returns 401")
    void testUnauthenticatedDeleteReturns401() throws Exception {
        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{ids} - Invalid ID format returns 400")
    void testDeleteInvalidIdFormatReturns400() throws Exception {
        mockMvc.perform(delete("/api/v1/users/abc")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid ID value 'abc'")));

        mockMvc.perform(delete("/api/v1/users/0")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("must be greater than zero")));

        mockMvc.perform(delete("/api/v1/users/-5")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("must be greater than zero")));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{ids} - Cannot delete user with stock transaction history (409)")
    void testDeleteUserWithStockTransactionsReturns409() throws Exception {
        long ts = System.currentTimeMillis();
        AppUser u1 = userRepository.save(AppUser.builder().username("tx_user_" + ts).password("Pass@123").fullName("Tx User").role(Role.STAFF).build());
        Category cat = categoryRepository.save(Category.builder().name("Cat_" + ts).build());
        Product prod = productRepository.save(Product.builder()
                .name("Prod_" + ts)
                .sku("SKU_" + ts)
                .category(cat)
                .unitPrice(new BigDecimal("25.00"))
                .stockQuantity(10)
                .minimumStock(1)
                .active(true)
                .build());

        stockTransactionRepository.save(StockTransaction.builder()
                .product(prod)
                .transactionType(TransactionType.STOCK_IN)
                .quantity(10)
                .quantityBefore(0)
                .quantityAfter(10)
                .note("Initial stock")
                .createdBy(u1)
                .build());

        mockMvc.perform(delete("/api/v1/users/" + u1.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("stock transaction records")))
                .andExpect(jsonPath("$.message", containsString("Nothing was deleted")));

        org.junit.jupiter.api.Assertions.assertTrue(userRepository.existsById(u1.getId()), "User with transactions must NOT be deleted");
    }
}
