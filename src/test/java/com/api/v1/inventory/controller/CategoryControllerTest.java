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
                .andExpect(jsonPath("$.message", is("Category deleted successfully")));

        mockMvc.perform(get("/api/v1/categories/" + category.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{ids} - Bulk delete multiple categories successfully")
    void testAdminDeletesMultipleCategoriesSuccessfully() throws Exception {
        long ts = System.currentTimeMillis();
        Category c1 = categoryRepository.save(Category.builder().name("BulkCat1_" + ts).build());
        Category c2 = categoryRepository.save(Category.builder().name("BulkCat2_" + ts).build());
        Category c3 = categoryRepository.save(Category.builder().name("BulkCat3_" + ts).build());

        mockMvc.perform(delete("/api/v1/categories/" + c1.getId() + "," + c2.getId() + "," + c3.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Categories deleted successfully")));

        org.junit.jupiter.api.Assertions.assertFalse(categoryRepository.existsById(c1.getId()));
        org.junit.jupiter.api.Assertions.assertFalse(categoryRepository.existsById(c2.getId()));
        org.junit.jupiter.api.Assertions.assertFalse(categoryRepository.existsById(c3.getId()));
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{ids} - Empty entries like '1,2,,3' are skipped")
    void testBulkDeleteSkipsEmptyEntries() throws Exception {
        long ts = System.currentTimeMillis();
        Category c1 = categoryRepository.save(Category.builder().name("SkipEmpty1_" + ts).build());
        Category c2 = categoryRepository.save(Category.builder().name("SkipEmpty2_" + ts).build());

        mockMvc.perform(delete("/api/v1/categories/" + c1.getId() + ",," + c2.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Categories deleted successfully")));

        org.junit.jupiter.api.Assertions.assertFalse(categoryRepository.existsById(c1.getId()));
        org.junit.jupiter.api.Assertions.assertFalse(categoryRepository.existsById(c2.getId()));
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{ids} - Duplicate IDs are handled cleanly")
    void testBulkDeleteHandlesDuplicateIds() throws Exception {
        long ts = System.currentTimeMillis();
        Category c1 = categoryRepository.save(Category.builder().name("DupCat1_" + ts).build());
        Category c2 = categoryRepository.save(Category.builder().name("DupCat2_" + ts).build());

        mockMvc.perform(delete("/api/v1/categories/" + c1.getId() + "," + c1.getId() + "," + c2.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Categories deleted successfully")));

        org.junit.jupiter.api.Assertions.assertFalse(categoryRepository.existsById(c1.getId()));
        org.junit.jupiter.api.Assertions.assertFalse(categoryRepository.existsById(c2.getId()));
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{ids} - Non-numeric ID returns 400")
    void testBulkDeleteNonNumericReturns400() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/abc")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("positive numbers")));
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{ids} - Zero or negative ID returns 400")
    void testBulkDeleteZeroOrNegativeReturns400() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/0")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("greater than zero")));

        mockMvc.perform(delete("/api/v1/categories/-1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("greater than zero")));
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{ids} - Only commas returns 400")
    void testBulkDeleteOnlyCommasReturns400() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/,")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("No valid IDs provided")));
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{ids} - More than 100 IDs returns 400")
    void testBulkDeleteMoreThan100IdsReturns400() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 101; i++) {
            if (i > 1) sb.append(",");
            sb.append(i);
        }

        mockMvc.perform(delete("/api/v1/categories/" + sb)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Cannot process more than 100 IDs")));
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{ids} - One missing ID returns 404 and deletes nothing")
    void testBulkDeleteOneMissingIdReturns404AndDeletesNothing() throws Exception {
        Category existing = categoryRepository.save(Category.builder().name("ExistingCat_" + System.currentTimeMillis()).build());

        mockMvc.perform(delete("/api/v1/categories/" + existing.getId() + ",999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Category not found with id: 999999")))
                .andExpect(jsonPath("$.message", containsString("Nothing was deleted")));

        org.junit.jupiter.api.Assertions.assertTrue(categoryRepository.existsById(existing.getId()), "Existing category must NOT be deleted");
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{ids} - Several missing IDs returns 404 listing all and deletes nothing")
    void testBulkDeleteSeveralMissingIdsReturns404ListingAll() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/999991,999992")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Categories not found with IDs: [999991, 999992]")))
                .andExpect(jsonPath("$.message", containsString("Nothing was deleted")));
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{ids} - Referenced category returns 409 and deletes nothing")
    void testBulkDeleteReferencedCategoryReturns409AndDeletesNothing() throws Exception {
        long ts = System.currentTimeMillis();
        Category freeCat = categoryRepository.save(Category.builder().name("FreeCat_" + ts).build());
        Category refCat = categoryRepository.save(Category.builder().name("RefCat_" + ts).build());

        productRepository.save(Product.builder()
                .sku("SKU-BULK-CAT-" + ts)
                .name("Ref Mouse")
                .category(refCat)
                .unitPrice(new BigDecimal("19.99"))
                .stockQuantity(10)
                .minimumStock(2)
                .active(true)
                .build());

        mockMvc.perform(delete("/api/v1/categories/" + freeCat.getId() + "," + refCat.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("referenced by one or more products")))
                .andExpect(jsonPath("$.message", containsString("Nothing was deleted")));

        org.junit.jupiter.api.Assertions.assertTrue(categoryRepository.existsById(freeCat.getId()), "Free category must NOT be deleted");
        org.junit.jupiter.api.Assertions.assertTrue(categoryRepository.existsById(refCat.getId()), "Ref category must NOT be deleted");
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
    @DisplayName("DELETE /api/v1/categories/{id} - Unauthenticated request returns 401")
    void testUnauthenticatedDeleteCategoryReturns401() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{id} - Nonexistent category returns 404")
    void testDeleteNonExistentCategoryReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Nothing was deleted")));
    }
}
