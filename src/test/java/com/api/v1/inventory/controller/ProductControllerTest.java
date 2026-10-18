package com.api.v1.inventory.controller;

import com.api.v1.inventory.model.entity.AppUser;
import com.api.v1.inventory.model.entity.Category;
import com.api.v1.inventory.model.entity.Product;
import com.api.v1.inventory.model.entity.StockTransaction;
import com.api.v1.inventory.model.entity.Supplier;
import com.api.v1.inventory.model.enumerate.TransactionType;
import com.api.v1.inventory.model.request.CreateProductRequest;
import com.api.v1.inventory.model.request.LoginRequest;
import com.api.v1.inventory.model.request.UpdateProductRequest;
import com.api.v1.inventory.repository.AppUserRepository;
import com.api.v1.inventory.repository.CategoryRepository;
import com.api.v1.inventory.repository.ProductRepository;
import com.api.v1.inventory.repository.StockTransactionRepository;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    private String adminToken;
    private String staffToken;
    private Category sampleCategory;
    private Supplier sampleSupplier;

    @BeforeEach
    void setupData() throws Exception {
        adminToken = obtainToken("admin", "Admin@12345");
        staffToken = obtainToken("staff", "Staff@12345");

        long ts = System.currentTimeMillis();
        sampleCategory = categoryRepository.save(Category.builder()
                .name("ProdTestCat_" + ts)
                .description("Category for product testing")
                .build());

        sampleSupplier = supplierRepository.save(Supplier.builder()
                .name("ProdTestSup_" + ts)
                .phone("097" + (ts % 1000000))
                .email("sup" + ts + "@testprod.com")
                .address("Phnom Penh")
                .build());
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
    @DisplayName("POST /api/v1/products - Admin successfully creates product with zero initial stock")
    void testAdminCreatesProduct() throws Exception {
        long ts = System.currentTimeMillis();
        String sku = "SKU-TEST-" + ts;
        CreateProductRequest request = CreateProductRequest.builder()
                .sku(sku)
                .name("Gaming Keyboard " + ts)
                .description("Mechanical RGB keyboard")
                .categoryId(sampleCategory.getId())
                .supplierId(sampleSupplier.getId())
                .unitPrice(new BigDecimal("79.99"))
                .minimumStock(5)
                .active(true)
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.sku", is(sku)))
                .andExpect(jsonPath("$.data.stockQuantity", is(0)))
                .andExpect(jsonPath("$.data.lowStock", is(true)))
                .andExpect(jsonPath("$.data.category.id", is(sampleCategory.getId().intValue())))
                .andExpect(jsonPath("$.data.supplier.id", is(sampleSupplier.getId().intValue())));
    }

    @Test
    @DisplayName("POST /api/v1/products - Reject duplicate SKU case-insensitively")
    void testCreateProductDuplicateSkuRejected() throws Exception {
        long ts = System.currentTimeMillis();
        String sku = "SKU-DUP-" + ts;

        productRepository.save(Product.builder()
                .sku(sku)
                .name("Original Item")
                .category(sampleCategory)
                .unitPrice(new BigDecimal("10.00"))
                .stockQuantity(0)
                .minimumStock(1)
                .active(true)
                .build());

        CreateProductRequest request = CreateProductRequest.builder()
                .sku(sku.toLowerCase())
                .name("Duplicate SKU Item")
                .categoryId(sampleCategory.getId())
                .unitPrice(new BigDecimal("20.00"))
                .minimumStock(2)
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/v1/products - Reject invalid Category ID")
    void testCreateProductInvalidCategoryRejected() throws Exception {
        long ts = System.currentTimeMillis();
        CreateProductRequest request = CreateProductRequest.builder()
                .sku("SKU-INV-CAT-" + ts)
                .name("Invalid Category Product")
                .categoryId(999999L)
                .unitPrice(new BigDecimal("15.00"))
                .minimumStock(2)
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Category not found")));
    }

    @Test
    @DisplayName("POST /api/v1/products - Reject invalid Supplier ID")
    void testCreateProductInvalidSupplierRejected() throws Exception {
        long ts = System.currentTimeMillis();
        CreateProductRequest request = CreateProductRequest.builder()
                .sku("SKU-INV-SUP-" + ts)
                .name("Invalid Supplier Product")
                .categoryId(sampleCategory.getId())
                .supplierId(999999L)
                .unitPrice(new BigDecimal("15.00"))
                .minimumStock(2)
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Supplier not found")));
    }

    @Test
    @DisplayName("POST /api/v1/products - Validation rejects negative price and missing fields")
    void testCreateProductValidationFailures() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .sku("  ")
                .name("")
                .unitPrice(new BigDecimal("-10.00"))
                .minimumStock(-1)
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errors", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/products - Staff cannot create product")
    void testStaffCannotCreateProduct() throws Exception {
        long ts = System.currentTimeMillis();
        CreateProductRequest request = CreateProductRequest.builder()
                .sku("SKU-STAFF-" + ts)
                .name("Staff Product")
                .categoryId(sampleCategory.getId())
                .unitPrice(new BigDecimal("25.00"))
                .minimumStock(2)
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/products - Unauthenticated request returns 401")
    void testUnauthenticatedCannotCreateProduct() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .sku("SKU-NOAUTH")
                .name("No Auth")
                .categoryId(sampleCategory.getId())
                .unitPrice(new BigDecimal("10.00"))
                .minimumStock(1)
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/products - Admin and Staff can list, search, and filter products")
    void testAdminAndStaffCanListAndSearchProducts() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("categoryId", sampleCategory.getId().toString())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()));

        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Retrieve product by ID")
    void testAdminAndStaffCanGetProductById() throws Exception {
        long ts = System.currentTimeMillis();
        Product product = productRepository.save(Product.builder()
                .sku("SKU-GETID-" + ts)
                .name("Product for Get ID")
                .category(sampleCategory)
                .unitPrice(new BigDecimal("50.00"))
                .stockQuantity(15)
                .minimumStock(5)
                .active(true)
                .build());

        mockMvc.perform(get("/api/v1/products/" + product.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(product.getId().intValue())))
                .andExpect(jsonPath("$.data.sku", is(product.getSku())))
                .andExpect(jsonPath("$.data.lowStock", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/products/sku/{sku} - Retrieve product by SKU")
    void testAdminAndStaffCanGetProductBySku() throws Exception {
        long ts = System.currentTimeMillis();
        String sku = "SKU-FINDBYSKU-" + ts;
        Product product = productRepository.save(Product.builder()
                .sku(sku)
                .name("Find by SKU Item")
                .category(sampleCategory)
                .unitPrice(new BigDecimal("35.00"))
                .stockQuantity(2)
                .minimumStock(5)
                .active(true)
                .build());

        mockMvc.perform(get("/api/v1/products/sku/" + sku)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(product.getId().intValue())))
                .andExpect(jsonPath("$.data.lowStock", is(true)));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Nonexistent ID returns 404")
    void testGetNonExistentProductReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/products/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id} - Admin updates product successfully")
    void testAdminUpdatesProductSuccessfully() throws Exception {
        long ts = System.currentTimeMillis();
        Product product = productRepository.save(Product.builder()
                .sku("SKU-PREUPDATE-" + ts)
                .name("Pre Update Item")
                .category(sampleCategory)
                .unitPrice(new BigDecimal("20.00"))
                .stockQuantity(10)
                .minimumStock(2)
                .active(true)
                .build());

        UpdateProductRequest request = UpdateProductRequest.builder()
                .sku("SKU-POSTUPDATE-" + ts)
                .name("Post Update Name")
                .description("Updated description")
                .categoryId(sampleCategory.getId())
                .supplierId(sampleSupplier.getId())
                .unitPrice(new BigDecimal("39.99"))
                .minimumStock(5)
                .active(true)
                .build();

        mockMvc.perform(put("/api/v1/products/" + product.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.sku", is(request.getSku())))
                .andExpect(jsonPath("$.data.name", is(request.getName())))
                .andExpect(jsonPath("$.data.unitPrice", is(39.99)))
                .andExpect(jsonPath("$.data.stockQuantity", is(10))); // Preserved!
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id} - General product update cannot modify stock quantity")
    void testUpdateProductCannotChangeStockQuantity() throws Exception {
        long ts = System.currentTimeMillis();
        Product product = productRepository.save(Product.builder()
                .sku("SKU-IMMUTABLE-STOCK-" + ts)
                .name("Protected Stock Product")
                .category(sampleCategory)
                .unitPrice(new BigDecimal("100.00"))
                .stockQuantity(42) // Original stock
                .minimumStock(5)
                .active(true)
                .build());

        // Attempting to send custom JSON with stockQuantity: 9999
        String jsonPayload = """
                {
                    "sku": "%s",
                    "name": "Protected Stock Product Renamed",
                    "categoryId": %d,
                    "unitPrice": 105.00,
                    "minimumStock": 5,
                    "active": true,
                    "stockQuantity": 9999
                }
                """.formatted(product.getSku(), sampleCategory.getId());

        mockMvc.perform(put("/api/v1/products/" + product.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.stockQuantity", is(42))); // Stock MUST remain 42!

        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(42, reloaded.getStockQuantity(), "Stock quantity in database must remain 42");
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id} - Reject update with SKU already in use")
    void testUpdateProductDuplicateSkuRejected() throws Exception {
        long ts = System.currentTimeMillis();
        String existingSku = "SKU-EXISTING-" + ts;
        productRepository.save(Product.builder()
                .sku(existingSku)
                .name("Existing SKU Holder")
                .category(sampleCategory)
                .unitPrice(new BigDecimal("10.00"))
                .stockQuantity(0)
                .minimumStock(1)
                .active(true)
                .build());

        Product productToUpdate = productRepository.save(Product.builder()
                .sku("SKU-TOUPDATE-" + ts)
                .name("To Update Item")
                .category(sampleCategory)
                .unitPrice(new BigDecimal("15.00"))
                .stockQuantity(0)
                .minimumStock(1)
                .active(true)
                .build());

        UpdateProductRequest request = UpdateProductRequest.builder()
                .sku(existingSku.toLowerCase())
                .name("Colliding SKU Item")
                .categoryId(sampleCategory.getId())
                .unitPrice(new BigDecimal("15.00"))
                .minimumStock(1)
                .active(true)
                .build();

        mockMvc.perform(put("/api/v1/products/" + productToUpdate.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id} - Staff cannot update product")
    void testStaffCannotUpdateProduct() throws Exception {
        long ts = System.currentTimeMillis();
        Product product = productRepository.save(Product.builder()
                .sku("SKU-STAFFNOUPDATE-" + ts)
                .name("Staff Cannot Update")
                .category(sampleCategory)
                .unitPrice(new BigDecimal("20.00"))
                .stockQuantity(0)
                .minimumStock(1)
                .active(true)
                .build());

        UpdateProductRequest request = UpdateProductRequest.builder()
                .sku(product.getSku())
                .name("Hacked Name")
                .categoryId(sampleCategory.getId())
                .unitPrice(new BigDecimal("20.00"))
                .minimumStock(1)
                .active(true)
                .build();

        mockMvc.perform(put("/api/v1/products/" + product.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} - Admin deletes product with zero stock and no transactions")
    void testAdminDeletesProductWithoutTransactionsOrStock() throws Exception {
        long ts = System.currentTimeMillis();
        Product product = productRepository.save(Product.builder()
                .sku("SKU-TODELETE-" + ts)
                .name("To Delete Item")
                .category(sampleCategory)
                .unitPrice(new BigDecimal("10.00"))
                .stockQuantity(0)
                .minimumStock(1)
                .active(true)
                .build());

        mockMvc.perform(delete("/api/v1/products/" + product.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("deleted successfully")));

        mockMvc.perform(get("/api/v1/products/" + product.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} - Reject deletion when product has positive stock")
    void testAdminDeleteProductWithPositiveStockRejected() throws Exception {
        long ts = System.currentTimeMillis();
        Product product = productRepository.save(Product.builder()
                .sku("SKU-NONZEROSTOCK-" + ts)
                .name("Has Stock Item")
                .category(sampleCategory)
                .unitPrice(new BigDecimal("10.00"))
                .stockQuantity(25) // Non-zero stock
                .minimumStock(1)
                .active(true)
                .build());

        mockMvc.perform(delete("/api/v1/products/" + product.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("non-zero stock quantity")));
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} - Reject deletion when product has transaction history")
    void testAdminDeleteProductWithTransactionHistoryRejected() throws Exception {
        long ts = System.currentTimeMillis();
        Product product = productRepository.save(Product.builder()
                .sku("SKU-HASTRANSACTIONS-" + ts)
                .name("Has Transactions Item")
                .category(sampleCategory)
                .unitPrice(new BigDecimal("10.00"))
                .stockQuantity(0)
                .minimumStock(1)
                .active(true)
                .build());

        AppUser adminUser = appUserRepository.findByUsernameIgnoreCase("admin").orElseThrow();

        stockTransactionRepository.save(StockTransaction.builder()
                .product(product)
                .transactionType(TransactionType.STOCK_IN)
                .quantity(10)
                .quantityBefore(0)
                .quantityAfter(10)
                .note("Historical transaction record")
                .createdBy(adminUser)
                .build());

        mockMvc.perform(delete("/api/v1/products/" + product.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("associated transaction records")));
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} - Staff cannot delete product")
    void testStaffCannotDeleteProduct() throws Exception {
        long ts = System.currentTimeMillis();
        Product product = productRepository.save(Product.builder()
                .sku("SKU-STAFFNODELETE-" + ts)
                .name("Staff Cannot Delete Item")
                .category(sampleCategory)
                .unitPrice(new BigDecimal("10.00"))
                .stockQuantity(0)
                .minimumStock(1)
                .active(true)
                .build());

        mockMvc.perform(delete("/api/v1/products/" + product.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} - Nonexistent product returns 404")
    void testDeleteNonExistentProductReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/products/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id}/status - Admin successfully updates product status")
    void testAdminCanUpdateProductStatusSuccess() throws Exception {
        long ts = System.currentTimeMillis();
        Product product = productRepository.save(Product.builder()
                .sku("SKU-STATUS-OK-" + ts)
                .name("Status Toggle Item")
                .category(sampleCategory)
                .unitPrice(new BigDecimal("19.99"))
                .stockQuantity(5)
                .minimumStock(2)
                .active(true)
                .build());

        com.api.v1.inventory.model.request.UpdateProductStatusRequest deactivateReq =
                new com.api.v1.inventory.model.request.UpdateProductStatusRequest(false);

        mockMvc.perform(put("/api/v1/products/" + product.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deactivateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.active", is(false)));

        com.api.v1.inventory.model.request.UpdateProductStatusRequest activateReq =
                new com.api.v1.inventory.model.request.UpdateProductStatusRequest(true);

        mockMvc.perform(put("/api/v1/products/" + product.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.active", is(true)));
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id}/status - Staff is forbidden (403)")
    void testStaffCannotUpdateProductStatus() throws Exception {
        long ts = System.currentTimeMillis();
        Product product = productRepository.save(Product.builder()
                .sku("SKU-STATUS-STAFF-" + ts)
                .name("Staff Status Toggle Test")
                .category(sampleCategory)
                .unitPrice(new BigDecimal("15.00"))
                .stockQuantity(0)
                .minimumStock(1)
                .active(true)
                .build());

        com.api.v1.inventory.model.request.UpdateProductStatusRequest req =
                new com.api.v1.inventory.model.request.UpdateProductStatusRequest(false);

        mockMvc.perform(put("/api/v1/products/" + product.getId() + "/status")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id}/status - Nonexistent product returns 404")
    void testUpdateProductStatusNotFound() throws Exception {
        com.api.v1.inventory.model.request.UpdateProductStatusRequest req =
                new com.api.v1.inventory.model.request.UpdateProductStatusRequest(false);

        mockMvc.perform(put("/api/v1/products/999999/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id}/status - Invalid input (null active) returns 400 Bad Request")
    void testUpdateProductStatusInvalidInput() throws Exception {
        long ts = System.currentTimeMillis();
        Product product = productRepository.save(Product.builder()
                .sku("SKU-STATUS-INVALID-" + ts)
                .name("Invalid Status Input Test")
                .category(sampleCategory)
                .unitPrice(new BigDecimal("15.00"))
                .stockQuantity(0)
                .minimumStock(1)
                .active(true)
                .build());

        mockMvc.perform(put("/api/v1/products/" + product.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errors", notNullValue()));
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id}/status - Preserves all other product fields and stock quantity")
    void testUpdateProductStatusPreservesOtherFields() throws Exception {
        long ts = System.currentTimeMillis();
        String originalSku = "SKU-STATUS-PRESERVE-" + ts;
        Product product = productRepository.save(Product.builder()
                .sku(originalSku)
                .name("Preserved Fields Product")
                .description("Special description to verify preservation")
                .category(sampleCategory)
                .supplier(sampleSupplier)
                .unitPrice(new BigDecimal("45.50"))
                .stockQuantity(17) // Crucial: stock quantity must not be altered
                .minimumStock(8)
                .active(true)
                .build());

        com.api.v1.inventory.model.request.UpdateProductStatusRequest req =
                new com.api.v1.inventory.model.request.UpdateProductStatusRequest(false);

        mockMvc.perform(put("/api/v1/products/" + product.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.active", is(false)))
                .andExpect(jsonPath("$.data.sku", is(originalSku)))
                .andExpect(jsonPath("$.data.name", is("Preserved Fields Product")))
                .andExpect(jsonPath("$.data.unitPrice", is(45.50)))
                .andExpect(jsonPath("$.data.stockQuantity", is(17)))
                .andExpect(jsonPath("$.data.minimumStock", is(8)));

        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(false, reloaded.isActive());
        assertEquals(17, reloaded.getStockQuantity(), "Stock quantity must be completely unchanged");
        assertEquals(new BigDecimal("45.50"), reloaded.getUnitPrice(), "Unit price must be unchanged");
        assertEquals(originalSku, reloaded.getSku(), "SKU must be unchanged");
        assertEquals("Preserved Fields Product", reloaded.getName(), "Name must be unchanged");
    }

    @Test
    @DisplayName("POST /api/v1/products - Create product with explicit UOM (BOX)")
    void testCreateProductWithExplicitUom() throws Exception {
        long ts = System.currentTimeMillis();
        CreateProductRequest request = CreateProductRequest.builder()
                .sku("SKU-UOM-BOX-" + ts)
                .name("Boxed Item")
                .categoryId(sampleCategory.getId())
                .unitOfMeasure(com.api.v1.inventory.model.enumerate.UnitOfMeasure.BOX)
                .unitPrice(new BigDecimal("120.00"))
                .minimumStock(10)
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.unitOfMeasure", is("BOX")));
    }

    @Test
    @DisplayName("POST /api/v1/products - Create product with omitted UOM defaults to PCS")
    void testCreateProductWithDefaultUom() throws Exception {
        long ts = System.currentTimeMillis();
        String json = """
                {
                    "sku": "SKU-UOM-DEFAULT-%d",
                    "name": "Default UOM Item",
                    "categoryId": %d,
                    "unitPrice": 50.00,
                    "minimumStock": 5
                }
                """.formatted(ts, sampleCategory.getId());

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.unitOfMeasure", is("PCS")));
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id} - Update product UOM from PCS to KG")
    void testUpdateProductUom() throws Exception {
        long ts = System.currentTimeMillis();
        Product product = productRepository.save(Product.builder()
                .sku("SKU-UOM-TOUPDATE-" + ts)
                .name("Item Before UOM Update")
                .category(sampleCategory)
                .unitOfMeasure(com.api.v1.inventory.model.enumerate.UnitOfMeasure.PCS)
                .unitPrice(new BigDecimal("30.00"))
                .stockQuantity(0)
                .minimumStock(2)
                .active(true)
                .build());

        UpdateProductRequest updateReq = UpdateProductRequest.builder()
                .sku(product.getSku())
                .name("Item After UOM Update")
                .categoryId(sampleCategory.getId())
                .unitOfMeasure(com.api.v1.inventory.model.enumerate.UnitOfMeasure.KG)
                .unitPrice(new BigDecimal("35.00"))
                .minimumStock(2)
                .active(true)
                .build();

        mockMvc.perform(put("/api/v1/products/" + product.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.unitOfMeasure", is("KG")));
    }

    @Test
    @DisplayName("POST /api/v1/products - Reject invalid UOM string with 400 Bad Request")
    void testCreateProductWithInvalidUomRejected() throws Exception {
        long ts = System.currentTimeMillis();
        String json = """
                {
                    "sku": "SKU-UOM-INVALID-%d",
                    "name": "Invalid UOM Item",
                    "categoryId": %d,
                    "unitOfMeasure": "INVALID_UNIT",
                    "unitPrice": 50.00,
                    "minimumStock": 5
                }
                """.formatted(ts, sampleCategory.getId());

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/products - unitOfMeasure 'ABC' returns 400 with allowed enum values")
    void testCreateProduct_UnitOfMeasureABC() throws Exception {
        String json = """
                {
                    "sku": "SKU-UOM-ABC-%d",
                    "name": "Invalid Enum ABC",
                    "categoryId": %d,
                    "unitOfMeasure": "ABC",
                    "unitPrice": 10.00,
                    "minimumStock": 5
                }
                """.formatted(System.currentTimeMillis(), sampleCategory.getId());

        MvcResult result = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid unitOfMeasure 'ABC'. Allowed: [PCS, BOX, PACK, BOTTLE, CAN, KG, LITER]")))
                .andReturn();
        System.out.println("RESPONSE (unitOfMeasure 'ABC'): " + result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("POST /api/v1/products - unitOfMeasure 'pcs' returns 400 with allowed enum values")
    void testCreateProduct_UnitOfMeasurePcs() throws Exception {
        String json = """
                {
                    "sku": "SKU-UOM-PCS-%d",
                    "name": "Invalid Enum pcs",
                    "categoryId": %d,
                    "unitOfMeasure": "pcs",
                    "unitPrice": 10.00,
                    "minimumStock": 5
                }
                """.formatted(System.currentTimeMillis(), sampleCategory.getId());

        MvcResult result = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid unitOfMeasure 'pcs'. Allowed: [PCS, BOX, PACK, BOTTLE, CAN, KG, LITER]")))
                .andReturn();
        System.out.println("RESPONSE (unitOfMeasure 'pcs'): " + result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("POST /api/v1/products - unitPrice 'abc' returns 400 with expected type")
    void testCreateProduct_UnitPriceAbc() throws Exception {
        String json = """
                {
                    "sku": "SKU-PRICE-ABC-%d",
                    "name": "Invalid Price abc",
                    "categoryId": %d,
                    "unitOfMeasure": "PCS",
                    "unitPrice": "abc",
                    "minimumStock": 5
                }
                """.formatted(System.currentTimeMillis(), sampleCategory.getId());

        MvcResult result = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid unitPrice 'abc'. Expected BigDecimal.")))
                .andReturn();
        System.out.println("RESPONSE (unitPrice 'abc'): " + result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("POST /api/v1/products - truly broken JSON returns 400 with fallback message")
    void testCreateProduct_TrulyBrokenJson() throws Exception {
        String json = "{ \"sku\": \"SKU-BROKEN\", \"name\": ";

        MvcResult result = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Malformed JSON request or invalid field format.")))
                .andReturn();
        System.out.println("RESPONSE (truly broken JSON): " + result.getResponse().getContentAsString());
    }
}

