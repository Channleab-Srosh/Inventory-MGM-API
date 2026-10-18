package com.api.v1.inventory.controller;

import com.api.v1.inventory.model.entity.Category;
import com.api.v1.inventory.model.entity.Product;
import com.api.v1.inventory.model.entity.StockTransaction;
import com.api.v1.inventory.model.enumerate.TransactionType;
import com.api.v1.inventory.model.request.LoginRequest;
import com.api.v1.inventory.model.request.StockOperationRequest;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    private String adminToken;
    private String staffToken;
    private Product testProduct;
    private Product inactiveProduct;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtainToken("admin", "Admin@12345");
        staffToken = obtainToken("staff", "Staff@12345");

        Category category = categoryRepository.findByNameIgnoreCase("StockTestCategory")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("StockTestCategory")
                        .description("Category for stock tests")
                        .build()));

        testProduct = productRepository.findBySkuIgnoreCase("STOCK-TEST-001")
                .orElseGet(() -> productRepository.save(Product.builder()
                        .sku("STOCK-TEST-001")
                        .name("Stock Test Item")
                        .category(category)
                        .unitPrice(BigDecimal.valueOf(25.00))
                        .stockQuantity(10)
                        .minimumStock(5)
                        .active(true)
                        .build()));

        testProduct.setStockQuantity(10);
        testProduct.setActive(true);
        testProduct = productRepository.save(testProduct);

        inactiveProduct = productRepository.findBySkuIgnoreCase("STOCK-INACTIVE-002")
                .orElseGet(() -> productRepository.save(Product.builder()
                        .sku("STOCK-INACTIVE-002")
                        .name("Inactive Stock Item")
                        .category(category)
                        .unitPrice(BigDecimal.valueOf(50.00))
                        .stockQuantity(5)
                        .minimumStock(2)
                        .active(false)
                        .build()));

        inactiveProduct.setActive(false);
        inactiveProduct = productRepository.save(inactiveProduct);
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
                .path("data")
                .path("token")
                .asText();
    }

    @Test
    @DisplayName("POST /api/v1/stock/in - Success increases stock and creates transaction record")
    void testStockInSuccess() throws Exception {
        StockOperationRequest request = StockOperationRequest.builder()
                .productId(testProduct.getId())
                .quantity(15)
                .note("Shipment received from supplier")
                .build();

        mockMvc.perform(post("/api/v1/stock/in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Stock added successfully")))
                .andExpect(jsonPath("$.data.productId", is(testProduct.getId().intValue())))
                .andExpect(jsonPath("$.data.transactionType", is("STOCK_IN")))
                .andExpect(jsonPath("$.data.quantity", is(15)))
                .andExpect(jsonPath("$.data.quantityBefore", is(10)))
                .andExpect(jsonPath("$.data.quantityAfter", is(25)))
                .andExpect(jsonPath("$.data.note", is("Shipment received from supplier")))
                .andExpect(jsonPath("$.data.createdByUsername", is("admin")));

        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(25, updatedProduct.getStockQuantity());
    }

    @Test
    @DisplayName("POST /api/v1/stock/in - Success by Staff role")
    void testStockInByStaff() throws Exception {
        StockOperationRequest request = StockOperationRequest.builder()
                .productId(testProduct.getId())
                .quantity(5)
                .note("Staff stock in")
                .build();

        mockMvc.perform(post("/api/v1/stock/in")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.createdByUsername", is("staff")));
    }

    @Test
    @DisplayName("POST /api/v1/stock/out - Success decreases stock and creates transaction record")
    void testStockOutSuccess() throws Exception {
        StockOperationRequest request = StockOperationRequest.builder()
                .productId(testProduct.getId())
                .quantity(4)
                .note("Customer sale")
                .build();

        mockMvc.perform(post("/api/v1/stock/out")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Stock deducted successfully")))
                .andExpect(jsonPath("$.data.productId", is(testProduct.getId().intValue())))
                .andExpect(jsonPath("$.data.transactionType", is("STOCK_OUT")))
                .andExpect(jsonPath("$.data.quantity", is(4)))
                .andExpect(jsonPath("$.data.quantityBefore", is(10)))
                .andExpect(jsonPath("$.data.quantityAfter", is(6)))
                .andExpect(jsonPath("$.data.note", is("Customer sale")))
                .andExpect(jsonPath("$.data.createdByUsername", is("staff")));

        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(6, updatedProduct.getStockQuantity());
    }

    @Test
    @DisplayName("POST /api/v1/stock/out - Rejects when requested quantity exceeds available stock (409 Conflict)")
    void testStockOutInsufficientStock() throws Exception {
        StockOperationRequest request = StockOperationRequest.builder()
                .productId(testProduct.getId())
                .quantity(20) // testProduct only has 10
                .note("Excessive deduction attempt")
                .build();

        mockMvc.perform(post("/api/v1/stock/out")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Insufficient stock")));

        // Verify stock quantity remains unchanged
        Product unchanged = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(10, unchanged.getStockQuantity());
    }

    @Test
    @DisplayName("POST /api/v1/stock/in - Rejects zero quantity (400 Bad Request)")
    void testStockInZeroQuantity() throws Exception {
        StockOperationRequest request = StockOperationRequest.builder()
                .productId(testProduct.getId())
                .quantity(0)
                .build();

        mockMvc.perform(post("/api/v1/stock/in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/stock/out - Rejects negative quantity (400 Bad Request)")
    void testStockOutNegativeQuantity() throws Exception {
        StockOperationRequest request = StockOperationRequest.builder()
                .productId(testProduct.getId())
                .quantity(-5)
                .build();

        mockMvc.perform(post("/api/v1/stock/out")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/stock/in - Rejects nonexistent product (404 Not Found)")
    void testStockInNonexistentProduct() throws Exception {
        StockOperationRequest request = StockOperationRequest.builder()
                .productId(999999L)
                .quantity(10)
                .build();

        mockMvc.perform(post("/api/v1/stock/in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Product not found")));
    }

    @Test
    @DisplayName("POST /api/v1/stock/in - Rejects inactive product (400 Bad Request)")
    void testStockInInactiveProduct() throws Exception {
        StockOperationRequest request = StockOperationRequest.builder()
                .productId(inactiveProduct.getId())
                .quantity(10)
                .build();

        mockMvc.perform(post("/api/v1/stock/in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("inactive product")));
    }

    @Test
    @DisplayName("POST /api/v1/stock/in - Rejects unauthenticated access (401 Unauthorized)")
    void testStockInUnauthenticated() throws Exception {
        StockOperationRequest request = StockOperationRequest.builder()
                .productId(testProduct.getId())
                .quantity(10)
                .build();

        mockMvc.perform(post("/api/v1/stock/in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/stock/history - Returns paginated transaction history")
    void testGetStockHistory() throws Exception {
        // Trigger a stock in first
        StockOperationRequest stockInReq = StockOperationRequest.builder()
                .productId(testProduct.getId())
                .quantity(2)
                .note("History check in")
                .build();

        mockMvc.perform(post("/api/v1/stock/in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stockInReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/stock/history")
                        .header("Authorization", "Bearer " + staffToken)
                        .param("productId", testProduct.getId().toString())
                        .param("type", "STOCK_IN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].transactionType", is("STOCK_IN")));
    }

    @Test
    @DisplayName("GET /api/v1/stock/history - Rejects invalid date range (400 Bad Request)")
    void testGetStockHistoryInvalidDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/stock/history")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("fromDate", "2026-10-15T12:00:00Z")
                        .param("toDate", "2026-10-10T12:00:00Z")) // fromDate > toDate
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("fromDate must be before or equal to toDate")));
    }

    @Test
    @DisplayName("GET /api/v1/stock/history/{id} - Success returns transaction details")
    void testGetStockHistoryByIdSuccess() throws Exception {
        StockOperationRequest stockInReq = StockOperationRequest.builder()
                .productId(testProduct.getId())
                .quantity(7)
                .note("Specific tx test")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/stock/in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stockInReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long txId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/stock/history/" + txId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(txId.intValue())))
                .andExpect(jsonPath("$.data.quantity", is(7)))
                .andExpect(jsonPath("$.data.unitOfMeasure", is("PCS")))
                .andExpect(jsonPath("$.data.note", is("Specific tx test")));
    }

    @Test
    @DisplayName("GET /api/v1/stock/history/{id} - Nonexistent ID returns 404 Not Found")
    void testGetStockHistoryByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/stock/history/99999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
