package com.api.v1.inventory.controller;

import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.enumerate.ExportFormat;
import com.api.v1.inventory.model.enumerate.ReportPeriod;
import com.api.v1.inventory.model.enumerate.StockStatusFilter;
import com.api.v1.inventory.model.response.ExportedFile;
import com.api.v1.inventory.model.response.InventoryReportPageResponse;
import com.api.v1.inventory.model.response.InventoryReportResponse;
import com.api.v1.inventory.model.response.InventorySummaryReportResponse;
import com.api.v1.inventory.model.response.StockTransactionResponse;
import com.api.v1.inventory.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReportControllerStandaloneTest {

    private MockMvc mockMvc;

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reportController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/reports/inventory with stockStatus=OUT_OF_STOCK returns 200")
    void testGetInventoryReportWithStockStatus() throws Exception {
        when(reportService.getInventoryReport(any(), any(), any(), any(), eq(StockStatusFilter.OUT_OF_STOCK), any(), any(), any(), any(), any(), any()))
                .thenReturn(InventoryReportPageResponse.inventoryPageBuilder()
                        .content(List.of(
                                InventoryReportResponse.builder().id(1L).sku("SKU-001").name("Test Product").build()
                        ))
                        .pageNumber(0)
                        .pageSize(10)
                        .totalElements(1)
                        .totalPages(1)
                        .last(true)
                        .totalProducts(1)
                        .totalUnits(10)
                        .totalInventoryValuation(new java.math.BigDecimal("100.00"))
                        .build());

        mockMvc.perform(get("/api/v1/reports/inventory")
                        .param("stockStatus", "OUT_OF_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].sku").value("SKU-001"))
                .andExpect(jsonPath("$.data.totalProducts").value(1))
                .andExpect(jsonPath("$.data.totalUnits").value(10));

        verify(reportService).getInventoryReport(any(), any(), any(), any(), eq(StockStatusFilter.OUT_OF_STOCK), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/v1/reports/inventory/summary returns 200 with stock summary and category breakdown")
    void testGetInventorySummaryReport() throws Exception {
        InventorySummaryReportResponse mockSummary = InventorySummaryReportResponse.builder()
                .totalItems(10)
                .totalUnits(150)
                .totalInventoryValuation(java.math.BigDecimal.valueOf(3000.00))
                .inStockCount(7)
                .lowStockCount(2)
                .outOfStockCount(1)
                .stockSummary(InventorySummaryReportResponse.StockSummaryReport.builder()
                        .totalItems(10)
                        .totalUnits(150)
                        .totalInventoryValuation(java.math.BigDecimal.valueOf(3000.00))
                        .inStockCount(7)
                        .lowStockCount(2)
                        .outOfStockCount(1)
                        .status(InventorySummaryReportResponse.StockStatusCount.builder()
                                .inStock(7)
                                .lowStock(2)
                                .outOfStock(1)
                                .build())
                        .build())
                .categoryBreakdown(List.of(
                        InventorySummaryReportResponse.CategoryStockBreakdown.builder()
                                .categoryId(1L)
                                .categoryName("Electronics")
                                .totalItems(6)
                                .totalUnits(100)
                                .totalValuation(java.math.BigDecimal.valueOf(2000.00))
                                .build()
                ))
                .build();

        when(reportService.getInventorySummaryReport(any(), any(), any(), any(), eq(StockStatusFilter.LOW_STOCK), any(), any(), any(), any(), any()))
                .thenReturn(mockSummary);

        mockMvc.perform(get("/api/v1/reports/inventory/summary")
                        .param("stockStatus", "LOW_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalItems").value(10))
                .andExpect(jsonPath("$.data.totalUnits").value(150))
                .andExpect(jsonPath("$.data.stockSummary.status.inStock").value(7))
                .andExpect(jsonPath("$.data.stockSummary.status.lowStock").value(2))
                .andExpect(jsonPath("$.data.stockSummary.status.outOfStock").value(1))
                .andExpect(jsonPath("$.data.categoryBreakdown[0].categoryName").value("Electronics"))
                .andExpect(jsonPath("$.data.categoryBreakdown[0].totalItems").value(6))
                .andExpect(jsonPath("$.data.categoryBreakdown[0].totalUnits").value(100));

        verify(reportService).getInventorySummaryReport(any(), any(), any(), any(), eq(StockStatusFilter.LOW_STOCK), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/v1/reports/stock-transactions with period=TODAY returns 200")
    void testGetStockTransactionReportWithPeriod() throws Exception {
        when(reportService.getStockTransactionReport(any(), any(), any(), eq(ReportPeriod.TODAY), any(), any(), any(), any()))
                .thenReturn(PageResponse.from(new PageImpl<>(List.of(
                        StockTransactionResponse.builder().id(10L).quantity(5).build()
                ))));

        mockMvc.perform(get("/api/v1/reports/stock-transactions")
                        .param("period", "TODAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(10));

        verify(reportService).getStockTransactionReport(any(), any(), any(), eq(ReportPeriod.TODAY), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/v1/reports/inventory/export?format=EXCEL returns excel file download")
    void testExportInventoryExcel() throws Exception {
        ExportedFile file = ExportedFile.builder()
                .fileName("inventory_report_20261031.xlsx")
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .data(new byte[]{1, 2, 3})
                .build();

        when(reportService.exportInventoryReport(eq(ExportFormat.EXCEL), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(file);

        mockMvc.perform(get("/api/v1/reports/inventory/export")
                        .param("format", "EXCEL")
                        .param("stockStatus", "LOW_STOCK"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"inventory_report_20261031.xlsx\""))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    @DisplayName("GET /api/v1/reports/inventory/export?format=PDF returns pdf file download")
    void testExportInventoryPdf() throws Exception {
        ExportedFile file = ExportedFile.builder()
                .fileName("inventory_report_20261031.pdf")
                .contentType("application/pdf")
                .data(new byte[]{4, 5, 6})
                .build();

        when(reportService.exportInventoryReport(eq(ExportFormat.PDF), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(file);

        mockMvc.perform(get("/api/v1/reports/inventory/export")
                        .param("format", "PDF")
                        .param("stockStatus", "OUT_OF_STOCK"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"inventory_report_20261031.pdf\""))
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(new byte[]{4, 5, 6}));
    }

    @Test
    @DisplayName("GET /api/v1/reports/stock-transactions/export?format=EXCEL returns excel download")
    void testExportStockTransactionsExcel() throws Exception {
        ExportedFile file = ExportedFile.builder()
                .fileName("stock_transactions_20261031.xlsx")
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .data(new byte[]{7, 8, 9})
                .build();

        when(reportService.exportStockTransactionReport(eq(ExportFormat.EXCEL), any(), any(), any(), eq(ReportPeriod.THIS_MONTH), any(), any(), any()))
                .thenReturn(file);

        mockMvc.perform(get("/api/v1/reports/stock-transactions/export")
                        .param("format", "EXCEL")
                        .param("period", "THIS_MONTH"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"stock_transactions_20261031.xlsx\""))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }
}
