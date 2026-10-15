package com.api.v1.inventory.controller;

import com.api.v1.inventory.model.entity.Category;
import com.api.v1.inventory.model.entity.Product;
import com.api.v1.inventory.model.entity.Supplier;
import com.api.v1.inventory.model.request.CreateSupplierRequest;
import com.api.v1.inventory.model.request.LoginRequest;
import com.api.v1.inventory.model.request.UpdateSupplierRequest;
import com.api.v1.inventory.repository.CategoryRepository;
import com.api.v1.inventory.repository.ProductRepository;
import com.api.v1.inventory.repository.SupplierRepository;
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
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SupplierRepository supplierRepository;

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
    @DisplayName("POST /api/v1/suppliers - Admin successfully creates a new supplier")
    void testAdminCreatesSupplier() throws Exception {
        long ts = System.currentTimeMillis();
        CreateSupplierRequest request = CreateSupplierRequest.builder()
                .name("Supplier_" + ts)
                .phone("0981" + (ts % 100000))
                .email("sup" + ts + "@supplier.com")
                .address("100 Industrial Park")
                .build();

        mockMvc.perform(post("/api/v1/suppliers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is(request.getName())))
                .andExpect(jsonPath("$.data.phone", is(request.getPhone())))
                .andExpect(jsonPath("$.data.email", is(request.getEmail())))
                .andExpect(jsonPath("$.data.address", is(request.getAddress())));
    }

    @Test
    @DisplayName("POST /api/v1/suppliers - Reject duplicate supplier name")
    void testCreateDuplicateSupplierNameRejected() throws Exception {
        long ts = System.currentTimeMillis();
        String name = "DupSup_" + ts;
        supplierRepository.save(Supplier.builder()
                .name(name)
                .phone("0982" + (ts % 100000))
                .build());

        CreateSupplierRequest request = CreateSupplierRequest.builder()
                .name(name)
                .phone("0983" + (ts % 100000))
                .build();

        mockMvc.perform(post("/api/v1/suppliers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/v1/suppliers - Reject duplicate supplier phone")
    void testCreateDuplicateSupplierPhoneRejected() throws Exception {
        long ts = System.currentTimeMillis();
        String phone = "0984" + (ts % 100000);
        supplierRepository.save(Supplier.builder()
                .name("UniqueSup1_" + ts)
                .phone(phone)
                .build());

        CreateSupplierRequest request = CreateSupplierRequest.builder()
                .name("UniqueSup2_" + ts)
                .phone(phone)
                .build();

        mockMvc.perform(post("/api/v1/suppliers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/v1/suppliers - Reject blank name and invalid phone")
    void testCreateSupplierValidationInvalidPhoneOrBlankName() throws Exception {
        CreateSupplierRequest request = CreateSupplierRequest.builder()
                .name("  ")
                .phone("bad#phone")
                .email("not-an-email")
                .build();

        mockMvc.perform(post("/api/v1/suppliers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errors", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/suppliers - Staff cannot create supplier")
    void testStaffCannotCreateSupplier() throws Exception {
        long ts = System.currentTimeMillis();
        CreateSupplierRequest request = CreateSupplierRequest.builder()
                .name("StaffSup_" + ts)
                .phone("0985" + (ts % 100000))
                .build();

        mockMvc.perform(post("/api/v1/suppliers")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/suppliers - Unauthenticated returns 401")
    void testUnauthenticatedCannotCreateSupplier() throws Exception {
        CreateSupplierRequest request = CreateSupplierRequest.builder()
                .name("NoAuthSup")
                .phone("098612345")
                .build();

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/suppliers - Admin and Staff can list suppliers")
    void testAdminAndStaffCanListSuppliers() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()));

        mockMvc.perform(get("/api/v1/suppliers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/suppliers/{id} - Retrieve supplier by ID")
    void testGetSupplierById() throws Exception {
        long ts = System.currentTimeMillis();
        Supplier supplier = supplierRepository.save(Supplier.builder()
                .name("GetSup_" + ts)
                .phone("0987" + (ts % 100000))
                .email("get" + ts + "@test.com")
                .address("Phnom Penh")
                .build());

        mockMvc.perform(get("/api/v1/suppliers/" + supplier.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(supplier.getId().intValue())))
                .andExpect(jsonPath("$.data.name", is(supplier.getName())));
    }

    @Test
    @DisplayName("GET /api/v1/suppliers/{id} - Nonexistent supplier returns 404")
    void testGetNonExistentSupplierReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("PUT /api/v1/suppliers/{id} - Admin updates supplier successfully")
    void testAdminUpdatesSupplier() throws Exception {
        long ts = System.currentTimeMillis();
        Supplier supplier = supplierRepository.save(Supplier.builder()
                .name("PreUpdateSup_" + ts)
                .phone("0988" + (ts % 100000))
                .address("Old Address")
                .build());

        UpdateSupplierRequest request = UpdateSupplierRequest.builder()
                .name("PostUpdateSup_" + ts)
                .phone("0989" + (ts % 100000))
                .email("updated" + ts + "@test.com")
                .address("New Address 456")
                .build();

        mockMvc.perform(put("/api/v1/suppliers/" + supplier.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is(request.getName())))
                .andExpect(jsonPath("$.data.phone", is(request.getPhone())))
                .andExpect(jsonPath("$.data.address", is("New Address 456")));
    }

    @Test
    @DisplayName("PUT /api/v1/suppliers/{id} - Reject update with phone of another supplier")
    void testUpdateSupplierWithDuplicatePhoneRejected() throws Exception {
        long ts = System.currentTimeMillis();
        String existingPhone = "0990" + (ts % 100000);
        supplierRepository.save(Supplier.builder()
                .name("ExistingPhoneSup_" + ts)
                .phone(existingPhone)
                .build());

        Supplier toUpdate = supplierRepository.save(Supplier.builder()
                .name("ToUpdateSup_" + ts)
                .phone("0991" + (ts % 100000))
                .build());

        UpdateSupplierRequest request = UpdateSupplierRequest.builder()
                .name(toUpdate.getName())
                .phone(existingPhone)
                .build();

        mockMvc.perform(put("/api/v1/suppliers/" + toUpdate.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("PUT /api/v1/suppliers/{id} - Staff cannot update supplier")
    void testStaffCannotUpdateSupplier() throws Exception {
        long ts = System.currentTimeMillis();
        Supplier supplier = supplierRepository.save(Supplier.builder()
                .name("StaffNoUpdateSup_" + ts)
                .phone("0992" + (ts % 100000))
                .build());

        UpdateSupplierRequest request = UpdateSupplierRequest.builder()
                .name("HackedSupplier")
                .phone("099300000")
                .build();

        mockMvc.perform(put("/api/v1/suppliers/" + supplier.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("DELETE /api/v1/suppliers/{id} - Admin deletes unreferenced supplier successfully")
    void testAdminDeletesSupplierWithoutProducts() throws Exception {
        long ts = System.currentTimeMillis();
        Supplier supplier = supplierRepository.save(Supplier.builder()
                .name("ToDeleteSup_" + ts)
                .phone("0994" + (ts % 100000))
                .build());

        mockMvc.perform(delete("/api/v1/suppliers/" + supplier.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("deleted successfully")));

        mockMvc.perform(get("/api/v1/suppliers/" + supplier.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/suppliers/{id} - Staff cannot delete supplier")
    void testStaffCannotDeleteSupplier() throws Exception {
        long ts = System.currentTimeMillis();
        Supplier supplier = supplierRepository.save(Supplier.builder()
                .name("StaffNoDeleteSup_" + ts)
                .phone("0995" + (ts % 100000))
                .build());

        mockMvc.perform(delete("/api/v1/suppliers/" + supplier.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("DELETE /api/v1/suppliers/{id} - Reject deletion when referenced by product")
    void testDeleteSupplierReferencedByProductRejected() throws Exception {
        long ts = System.currentTimeMillis();
        Category category = categoryRepository.save(Category.builder()
                .name("SupRefCat_" + ts)
                .build());

        Supplier supplier = supplierRepository.save(Supplier.builder()
                .name("ReferencedSup_" + ts)
                .phone("0996" + (ts % 100000))
                .build());

        productRepository.save(Product.builder()
                .sku("SKU-SUP-REF-" + ts)
                .name("Ref Hardware")
                .category(category)
                .supplier(supplier)
                .unitPrice(new BigDecimal("49.99"))
                .stockQuantity(25)
                .minimumStock(5)
                .active(true)
                .build());

        mockMvc.perform(delete("/api/v1/suppliers/" + supplier.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("referenced by one or more products")));
    }

    @Test
    @DisplayName("DELETE /api/v1/suppliers/{id} - Nonexistent supplier returns 404")
    void testDeleteNonExistentSupplierReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/suppliers/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
