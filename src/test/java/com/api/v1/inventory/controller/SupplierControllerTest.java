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
                .email("sup" + ts + "@gmail.com")
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
                .email("get" + ts + "@gmail.com")
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
                .email("updated" + ts + "@gmail.com")
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
                .andExpect(jsonPath("$.message", is("Supplier deleted successfully")));

        mockMvc.perform(get("/api/v1/suppliers/" + supplier.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/suppliers/{ids} - Bulk delete multiple suppliers successfully")
    void testAdminDeletesMultipleSuppliersSuccessfully() throws Exception {
        long ts = System.currentTimeMillis();
        Supplier s1 = supplierRepository.save(Supplier.builder().name("BulkSup1_" + ts).phone("0911" + (ts % 100000)).build());
        Supplier s2 = supplierRepository.save(Supplier.builder().name("BulkSup2_" + ts).phone("0912" + (ts % 100000)).build());
        Supplier s3 = supplierRepository.save(Supplier.builder().name("BulkSup3_" + ts).phone("0913" + (ts % 100000)).build());

        mockMvc.perform(delete("/api/v1/suppliers/" + s1.getId() + "," + s2.getId() + "," + s3.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Suppliers deleted successfully")));

        org.junit.jupiter.api.Assertions.assertFalse(supplierRepository.existsById(s1.getId()));
        org.junit.jupiter.api.Assertions.assertFalse(supplierRepository.existsById(s2.getId()));
        org.junit.jupiter.api.Assertions.assertFalse(supplierRepository.existsById(s3.getId()));
    }

    @Test
    @DisplayName("DELETE /api/v1/suppliers/{ids} - Empty entries like '1,2,,3' are skipped")
    void testBulkDeleteSkipsEmptyEntries() throws Exception {
        long ts = System.currentTimeMillis();
        Supplier s1 = supplierRepository.save(Supplier.builder().name("SkipEmptySup1_" + ts).phone("0921" + (ts % 100000)).build());
        Supplier s2 = supplierRepository.save(Supplier.builder().name("SkipEmptySup2_" + ts).phone("0922" + (ts % 100000)).build());

        mockMvc.perform(delete("/api/v1/suppliers/" + s1.getId() + ",," + s2.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Suppliers deleted successfully")));

        org.junit.jupiter.api.Assertions.assertFalse(supplierRepository.existsById(s1.getId()));
        org.junit.jupiter.api.Assertions.assertFalse(supplierRepository.existsById(s2.getId()));
    }

    @Test
    @DisplayName("DELETE /api/v1/suppliers/{ids} - Duplicate IDs are handled cleanly")
    void testBulkDeleteHandlesDuplicateIds() throws Exception {
        long ts = System.currentTimeMillis();
        Supplier s1 = supplierRepository.save(Supplier.builder().name("DupSup1_" + ts).phone("0931" + (ts % 100000)).build());
        Supplier s2 = supplierRepository.save(Supplier.builder().name("DupSup2_" + ts).phone("0932" + (ts % 100000)).build());

        mockMvc.perform(delete("/api/v1/suppliers/" + s1.getId() + "," + s1.getId() + "," + s2.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Suppliers deleted successfully")));

        org.junit.jupiter.api.Assertions.assertFalse(supplierRepository.existsById(s1.getId()));
        org.junit.jupiter.api.Assertions.assertFalse(supplierRepository.existsById(s2.getId()));
    }

    @Test
    @DisplayName("DELETE /api/v1/suppliers/{ids} - Non-numeric ID returns 400")
    void testBulkDeleteNonNumericReturns400() throws Exception {
        mockMvc.perform(delete("/api/v1/suppliers/abc")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("positive numbers")));
    }

    @Test
    @DisplayName("DELETE /api/v1/suppliers/{ids} - Zero or negative ID returns 400")
    void testBulkDeleteZeroOrNegativeReturns400() throws Exception {
        mockMvc.perform(delete("/api/v1/suppliers/0")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("greater than zero")));

        mockMvc.perform(delete("/api/v1/suppliers/-1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("greater than zero")));
    }

    @Test
    @DisplayName("DELETE /api/v1/suppliers/{ids} - Only commas returns 400")
    void testBulkDeleteOnlyCommasReturns400() throws Exception {
        mockMvc.perform(delete("/api/v1/suppliers/,")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("No valid IDs provided")));
    }

    @Test
    @DisplayName("DELETE /api/v1/suppliers/{ids} - More than 100 IDs returns 400")
    void testBulkDeleteMoreThan100IdsReturns400() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 101; i++) {
            if (i > 1) sb.append(",");
            sb.append(i);
        }

        mockMvc.perform(delete("/api/v1/suppliers/" + sb)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Cannot process more than 100 IDs")));
    }

    @Test
    @DisplayName("DELETE /api/v1/suppliers/{ids} - One missing ID returns 404 and deletes nothing")
    void testBulkDeleteOneMissingIdReturns404AndDeletesNothing() throws Exception {
        long ts = System.currentTimeMillis();
        Supplier existing = supplierRepository.save(Supplier.builder().name("ExistingSup_" + ts).phone("0941" + (ts % 100000)).build());

        mockMvc.perform(delete("/api/v1/suppliers/" + existing.getId() + ",999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Supplier not found with id: 999999")))
                .andExpect(jsonPath("$.message", containsString("Nothing was deleted")));

        org.junit.jupiter.api.Assertions.assertTrue(supplierRepository.existsById(existing.getId()), "Existing supplier must NOT be deleted");
    }

    @Test
    @DisplayName("DELETE /api/v1/suppliers/{ids} - Several missing IDs returns 404 listing all and deletes nothing")
    void testBulkDeleteSeveralMissingIdsReturns404ListingAll() throws Exception {
        mockMvc.perform(delete("/api/v1/suppliers/999991,999992")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Suppliers not found with IDs: [999991, 999992]")))
                .andExpect(jsonPath("$.message", containsString("Nothing was deleted")));
    }

    @Test
    @DisplayName("DELETE /api/v1/suppliers/{ids} - Referenced supplier returns 409 and deletes nothing")
    void testBulkDeleteReferencedSupplierReturns409AndDeletesNothing() throws Exception {
        long ts = System.currentTimeMillis();
        Category category = categoryRepository.save(Category.builder().name("SupRefCat2_" + ts).build());
        Supplier freeSup = supplierRepository.save(Supplier.builder().name("FreeSup_" + ts).phone("0951" + (ts % 100000)).build());
        Supplier refSup = supplierRepository.save(Supplier.builder().name("RefSup_" + ts).phone("0952" + (ts % 100000)).build());

        productRepository.save(Product.builder()
                .sku("SKU-BULK-SUP-" + ts)
                .name("Ref Hardware Bulk")
                .category(category)
                .supplier(refSup)
                .unitPrice(new BigDecimal("49.99"))
                .stockQuantity(25)
                .minimumStock(5)
                .active(true)
                .build());

        mockMvc.perform(delete("/api/v1/suppliers/" + freeSup.getId() + "," + refSup.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("referenced by one or more products")))
                .andExpect(jsonPath("$.message", containsString("Nothing was deleted")));

        org.junit.jupiter.api.Assertions.assertTrue(supplierRepository.existsById(freeSup.getId()), "Free supplier must NOT be deleted");
        org.junit.jupiter.api.Assertions.assertTrue(supplierRepository.existsById(refSup.getId()), "Ref supplier must NOT be deleted");
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
    @DisplayName("DELETE /api/v1/suppliers/{id} - Unauthenticated request returns 401")
    void testUnauthenticatedDeleteSupplierReturns401() throws Exception {
        mockMvc.perform(delete("/api/v1/suppliers/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("DELETE /api/v1/suppliers/{id} - Nonexistent supplier returns 404")
    void testDeleteNonExistentSupplierReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/suppliers/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Nothing was deleted")));
    }
}
