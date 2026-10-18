package com.api.v1.inventory.controller;

import com.api.v1.inventory.model.entity.Category;
import com.api.v1.inventory.model.entity.Product;
import com.api.v1.inventory.model.entity.Supplier;
import com.api.v1.inventory.model.enumerate.TransactionType;
import com.api.v1.inventory.model.request.LoginRequest;
import com.api.v1.inventory.model.request.StockOperationRequest;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest {

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

    private String adminToken;
    private String staffToken;
    private Product reportProduct;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtainToken("admin", "Admin@12345");
        staffToken = obtainToken("staff", "Staff@12345");

        Category category = categoryRepository.findByNameIgnoreCase("ReportCategory")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("ReportCategory")
                        .description("Report testing category")
                        .build()));

        Supplier supplier = supplierRepository.findByNameIgnoreCase("ReportSupplier")
                .orElseGet(() -> supplierRepository.save(Supplier.builder()
                        .name("ReportSupplier")
                        .phone("+85512777666")
                        .email("reportsupplier@test.com")
                        .build()));

        reportProduct = productRepository.findBySkuIgnoreCase("REP-PROD-001")
                .orElseGet(() -> productRepository.save(Product.builder()
                        .sku("REP-PROD-001")
                        .name("Report Sample Product")
                        .category(category)
                        .supplier(supplier)
                        .unitPrice(BigDecimal.valueOf(20.00))
                        .stockQuantity(15)
                        .minimumStock(10)
                        .active(true)
                        .build()));

        // Perform a stock-in to have guaranteed transaction history
        StockOperationRequest inReq = StockOperationRequest.builder()
                .productId(reportProduct.getId())
                .quantity(5)
                .note("Report test stock in")
                .build();

        mockMvc.perform(post("/api/v1/stock/in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inReq)))
                .andExpect(status().isCreated());
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
    @DisplayName("GET /api/v1/reports/stock-transactions - Admin can retrieve stock transaction report")
    void testGetStockTransactionReportAsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/reports/stock-transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("productId", reportProduct.getId().toString())
                        .param("type", TransactionType.STOCK_IN.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Stock transaction report retrieved successfully")))
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.content[0].productSku", is(reportProduct.getSku())));
    }

    @Test
    @DisplayName("GET /api/v1/reports/stock-transactions - Staff can retrieve stock transaction report")
    void testGetStockTransactionReportAsStaff() throws Exception {
        mockMvc.perform(get("/api/v1/reports/stock-transactions")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/reports/stock-transactions - Filter by valid date range")
    void testGetStockTransactionReportDateFilter() throws Exception {
        Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(1, ChronoUnit.DAYS);

        mockMvc.perform(get("/api/v1/reports/stock-transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("fromDate", from.toString())
                        .param("toDate", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("GET /api/v1/reports/stock-transactions - fromDate after toDate returns 400 Bad Request")
    void testGetStockTransactionReportInvalidDateRange() throws Exception {
        Instant from = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant to = Instant.now().minus(1, ChronoUnit.DAYS);

        mockMvc.perform(get("/api/v1/reports/stock-transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("fromDate", from.toString())
                        .param("toDate", to.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("fromDate must be before or equal to toDate")));
    }

    @Test
    @DisplayName("GET /api/v1/reports/stock-transactions - Unauthenticated returns 401 Unauthorized")
    void testGetStockTransactionReportUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/reports/stock-transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/reports/inventory - Admin can retrieve inventory valuation report")
    void testGetInventoryReportAsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/reports/inventory")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("keyword", "REP-PROD-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Inventory report retrieved successfully")))
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.content[0].sku", is("REP-PROD-001")))
                .andExpect(jsonPath("$.data.content[0].totalValue", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/reports/inventory - Staff can retrieve inventory report")
    void testGetInventoryReportAsStaff() throws Exception {
        mockMvc.perform(get("/api/v1/reports/inventory")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/reports/inventory - Filter with lowStockOnly=true")
    void testGetInventoryReportLowStockOnly() throws Exception {
        mockMvc.perform(get("/api/v1/reports/inventory")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("lowStockOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/reports/inventory - Unauthenticated returns 401 Unauthorized")
    void testGetInventoryReportUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/reports/inventory"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
