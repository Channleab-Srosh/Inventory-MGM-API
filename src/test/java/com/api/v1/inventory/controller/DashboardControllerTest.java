package com.api.v1.inventory.controller;

import com.api.v1.inventory.model.entity.Category;
import com.api.v1.inventory.model.entity.Product;
import com.api.v1.inventory.model.entity.Supplier;
import com.api.v1.inventory.model.request.LoginRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTest {

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

    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtainToken("admin", "Admin@12345");
        staffToken = obtainToken("staff", "Staff@12345");

        Category category = categoryRepository.findByNameIgnoreCase("DashCategory")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("DashCategory")
                        .description("Dashboard test category")
                        .build()));

        Supplier supplier = supplierRepository.findByNameIgnoreCase("DashSupplier")
                .orElseGet(() -> supplierRepository.save(Supplier.builder()
                        .name("DashSupplier")
                        .phone("+85512999888")
                        .email("dashsupplier@test.com")
                        .build()));

        Product lowProduct = productRepository.findBySkuIgnoreCase("DASH-PROD-LOW")
                .orElseGet(() -> Product.builder()
                        .sku("DASH-PROD-LOW")
                        .name("Dash Low Stock Item")
                        .category(category)
                        .supplier(supplier)
                        .unitPrice(BigDecimal.valueOf(50.00))
                        .build());
        lowProduct.setStockQuantity(1);
        lowProduct.setMinimumStock(10);
        lowProduct.setActive(true);
        productRepository.save(lowProduct);

        Product normalProduct = productRepository.findBySkuIgnoreCase("DASH-PROD-NORMAL")
                .orElseGet(() -> Product.builder()
                        .sku("DASH-PROD-NORMAL")
                        .name("Dash Normal Stock Item")
                        .category(category)
                        .supplier(supplier)
                        .unitPrice(BigDecimal.valueOf(100.00))
                        .build());
        normalProduct.setStockQuantity(20);
        normalProduct.setMinimumStock(5);
        normalProduct.setActive(true);
        productRepository.save(normalProduct);
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
    @DisplayName("GET /api/v1/dashboard/summary - Admin can retrieve KPI summary")
    void testGetSummaryAsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Dashboard summary retrieved successfully")))
                .andExpect(jsonPath("$.data.totalProducts", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.totalCategories", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalSuppliers", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalStockQuantity", greaterThanOrEqualTo(21)))
                .andExpect(jsonPath("$.data.totalInventoryValue", notNullValue()))
                .andExpect(jsonPath("$.data.lowStockCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.outOfStockCount", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.totalStockInQuantity", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.totalStockOutQuantity", greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/summary - Staff can retrieve KPI summary")
    void testGetSummaryAsStaff() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalProducts", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/summary - Unauthenticated returns 401 Unauthorized")
    void testGetSummaryUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/low-stock - Admin can retrieve low stock products")
    void testGetLowStockAsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/low-stock")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Low stock products retrieved successfully")))
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.content[0].lowStock", is(true)))
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/low-stock - Staff can retrieve low stock products")
    void testGetLowStockAsStaff() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/low-stock")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/low-stock - Unauthenticated returns 401 Unauthorized")
    void testGetLowStockUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/low-stock"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
