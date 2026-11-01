package com.api.v1.inventory.controller;

import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.enumerate.StockStatusFilter;
import com.api.v1.inventory.model.response.ProductResponse;
import com.api.v1.inventory.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerStandaloneTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/products with stockStatus=OUT_OF_STOCK filters products")
    void testGetProductsOutOfStock() throws Exception {
        ProductResponse prod = ProductResponse.builder()
                .id(1L)
                .sku("PROD-001")
                .name("Out of Stock Item")
                .stockQuantity(0)
                .minimumStock(5)
                .unitPrice(BigDecimal.valueOf(10.00))
                .build();

        when(productService.getProducts(any(), any(), any(), any(), eq(StockStatusFilter.OUT_OF_STOCK), any(), any()))
                .thenReturn(PageResponse.from(new PageImpl<>(List.of(prod))));

        mockMvc.perform(get("/api/v1/products")
                        .param("stockStatus", "OUT_OF_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].sku").value("PROD-001"))
                .andExpect(jsonPath("$.data.content[0].stockQuantity").value(0));

        verify(productService).getProducts(any(), any(), any(), any(), eq(StockStatusFilter.OUT_OF_STOCK), any(), any());
    }

    @Test
    @DisplayName("GET /api/v1/products with stockStatus=LOW_STOCK filters low stock products")
    void testGetProductsLowStock() throws Exception {
        ProductResponse prod = ProductResponse.builder()
                .id(2L)
                .sku("PROD-002")
                .name("Low Stock Item")
                .stockQuantity(2)
                .minimumStock(5)
                .unitPrice(BigDecimal.valueOf(25.00))
                .build();

        when(productService.getProducts(any(), any(), any(), any(), eq(StockStatusFilter.LOW_STOCK), any(), any()))
                .thenReturn(PageResponse.from(new PageImpl<>(List.of(prod))));

        mockMvc.perform(get("/api/v1/products")
                        .param("stockStatus", "LOW_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].sku").value("PROD-002"))
                .andExpect(jsonPath("$.data.content[0].stockQuantity").value(2));

        verify(productService).getProducts(any(), any(), any(), any(), eq(StockStatusFilter.LOW_STOCK), any(), any());
    }

    @Test
    @DisplayName("GET /api/v1/products/units returns all 19 units of measure")
    void testGetUnitsOfMeasure() throws Exception {
        mockMvc.perform(get("/api/v1/products/units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(19))
                .andExpect(jsonPath("$.data[0]").value("PCS"));
    }
}
