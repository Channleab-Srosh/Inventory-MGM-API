package com.api.v1.inventory.controller;

import com.api.v1.inventory.model.entity.Category;
import com.api.v1.inventory.model.entity.Product;
import com.api.v1.inventory.model.request.CreateCategoryRequest;
import com.api.v1.inventory.model.request.LoginRequest;
import com.api.v1.inventory.model.request.UpdateCategoryRequest;
import com.api.v1.inventory.repository.CategoryRepository;
import com.api.v1.inventory.repository.ProductRepository;
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
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

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
    @DisplayName("POST /api/v1/categories - Admin successfully creates category")
    void testAdminCreatesCategory() throws Exception {
        String name = "Category_" + System.currentTimeMillis();
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name(name)
                .description("Test description")
                .build();

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is(name)))
                .andExpect(jsonPath("$.data.description", is("Test description")));
    }

    @Test
    @DisplayName("POST /api/v1/categories - Reject duplicate category name case-insensitively")
    void testCreateDuplicateCategoryNameRejected() throws Exception {
        String name = "DupCat_" + System.currentTimeMillis();
        categoryRepository.save(Category.builder().name(name).build());

        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name(name.toLowerCase())
                .description("Duplicate check")
                .build();

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/v1/categories - Reject blank name")
    void testCreateCategoryValidationBlankName() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("   ")
                .description("Invalid")
                .build();

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/categories - Staff role is forbidden from creating category")
    void testStaffCannotCreateCategory() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Staff_Cat_" + System.currentTimeMillis())
                .build();

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/categories - Unauthenticated request returns 401")
    void testUnauthenticatedCannotCreateCategory() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("NoAuth_Cat")
                .build();

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/categories - Admin and Staff can list categories")
    void testAdminAndStaffCanListCategories() throws Exception {
        mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()));

        mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/categories/{id} - Retrieve category details by ID")
    void testGetCategoryById() throws Exception {
        Category category = categoryRepository.save(Category.builder()
                .name("GetById_" + System.currentTimeMillis())
                .description("Sample desc")
                .build());

        mockMvc.perform(get("/api/v1/categories/" + category.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(category.getId().intValue())))
                .andExpect(jsonPath("$.data.name", is(category.getName())));
    }

    @Test
    @DisplayName("GET /api/v1/categories/{id} - Nonexistent ID returns 404")
    void testGetNonExistentCategoryReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/categories/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("PUT /api/v1/categories/{id} - Admin updates category successfully")
    void testAdminUpdatesCategory() throws Exception {
        Category category = categoryRepository.save(Category.builder()
                .name("PreUpdate_" + System.currentTimeMillis())
                .description("Old")
                .build());

        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("PostUpdate_" + System.currentTimeMillis())
                .description("New Description")
                .build();

        mockMvc.perform(put("/api/v1/categories/" + category.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is(request.getName())))
                .andExpect(jsonPath("$.data.description", is("New Description")));
    }

    @Test
    @DisplayName("PUT /api/v1/categories/{id} - Update with name of another category rejected")
    void testUpdateCategoryWithDuplicateNameRejected() throws Exception {
        String existingName = "ExistingCat_" + System.currentTimeMillis();
        categoryRepository.save(Category.builder().name(existingName).build());

        Category toUpdate = categoryRepository.save(Category.builder()
                .name("ToUpdate_" + System.currentTimeMillis())
                .build());

        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name(existingName.toLowerCase())
                .build();

        mockMvc.perform(put("/api/v1/categories/" + toUpdate.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("PUT /api/v1/categories/{id} - Staff cannot update category")
    void testStaffCannotUpdateCategory() throws Exception {
        Category category = categoryRepository.save(Category.builder()
                .name("StaffCannotUpdate_" + System.currentTimeMillis())
                .build());

        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("HackedName")
                .build();

        mockMvc.perform(put("/api/v1/categories/" + category.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{id} - Admin deletes unreferenced category successfully")
    void testAdminDeletesCategoryWithoutProducts() throws Exception {
        Category category = categoryRepository.save(Category.builder()
                .name("ToDelete_" + System.currentTimeMillis())
                .build());

        mockMvc.perform(delete("/api/v1/categories/" + category.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("deleted successfully")));

        mockMvc.perform(get("/api/v1/categories/" + category.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{id} - Staff cannot delete category")
    void testStaffCannotDeleteCategory() throws Exception {
        Category category = categoryRepository.save(Category.builder()
                .name("StaffCannotDelete_" + System.currentTimeMillis())
                .build());

        mockMvc.perform(delete("/api/v1/categories/" + category.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{id} - Reject deletion when referenced by product")
    void testDeleteCategoryReferencedByProductRejected() throws Exception {
        Category category = categoryRepository.save(Category.builder()
                .name("ReferencedCat_" + System.currentTimeMillis())
                .build());

        productRepository.save(Product.builder()
                .sku("SKU-CAT-REF-" + System.currentTimeMillis())
                .name("Ref Mouse")
                .category(category)
                .unitPrice(new BigDecimal("19.99"))
                .stockQuantity(10)
                .minimumStock(2)
                .active(true)
                .build());

        mockMvc.perform(delete("/api/v1/categories/" + category.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("referenced by one or more products")));
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{id} - Nonexistent category returns 404")
    void testDeleteNonExistentCategoryReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
