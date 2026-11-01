package com.api.v1.inventory.service;

import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.entity.Category;
import com.api.v1.inventory.model.entity.Product;
import com.api.v1.inventory.model.entity.StockTransaction;
import com.api.v1.inventory.model.enumerate.ExportFormat;
import com.api.v1.inventory.model.enumerate.ReportPeriod;
import com.api.v1.inventory.model.enumerate.StockStatusFilter;
import com.api.v1.inventory.model.enumerate.TransactionType;
import com.api.v1.inventory.model.response.ExportedFile;
import com.api.v1.inventory.model.response.InventoryReportResponse;
import com.api.v1.inventory.model.response.StockTransactionResponse;
import com.api.v1.inventory.repository.ProductRepository;
import com.api.v1.inventory.repository.StockTransactionRepository;
import com.api.v1.inventory.service.impl.ReportServiceImpl;
import com.api.v1.inventory.service.report.ExcelReportService;
import com.api.v1.inventory.service.report.PdfReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private StockService stockService;

    @Mock
    private StockTransactionRepository stockTransactionRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ExcelReportService excelReportService;

    @Mock
    private PdfReportService pdfReportService;

    @InjectMocks
    private ReportServiceImpl reportService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        Category category = Category.builder().id(10L).name("Beverages").build();
        testProduct = Product.builder()
                .id(1L)
                .sku("BEV-001")
                .name("Mineral Water")
                .category(category)
                .unitPrice(BigDecimal.valueOf(1.50))
                .stockQuantity(0)
                .minimumStock(10)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should query inventory report with stockStatus OUT_OF_STOCK")
    void testGetInventoryReportOutOfStock() {
        Page<Product> page = new PageImpl<>(List.of(testProduct));
        when(productRepository.searchInventoryReport(
                isNull(), isNull(), isNull(), isNull(), eq("OUT_OF_STOCK"), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(page);
        when(productRepository.findInventoryReportList(
                isNull(), isNull(), isNull(), isNull(), eq("OUT_OF_STOCK"), isNull(), isNull(), isNull()
        )).thenReturn(List.of(testProduct));

        var response = reportService.getInventoryReport(
                null, null, null, null, StockStatusFilter.OUT_OF_STOCK, null, PageRequest.of(0, 10)
        );

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("BEV-001", response.getContent().get(0).getSku());
        assertEquals(1, response.getTotalProducts());
        assertEquals(0, response.getTotalUnits());
        assertEquals(1, response.getOutOfStockCount());
        verify(productRepository).searchInventoryReport(
                isNull(), isNull(), isNull(), isNull(), eq("OUT_OF_STOCK"), isNull(), isNull(), isNull(), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("Should calculate inventory summary report with stock and category breakdown")
    void testGetInventorySummaryReport() {
        Category cat = Category.builder().id(1L).name("Beverages").build();
        Product p1 = Product.builder()
                .id(1L).sku("BEV-001").name("Soda").category(cat)
                .unitPrice(BigDecimal.valueOf(2.00)).stockQuantity(20).minimumStock(10).active(true).build();
        Product p2 = Product.builder()
                .id(2L).sku("BEV-002").name("Water").category(cat)
                .unitPrice(BigDecimal.valueOf(1.00)).stockQuantity(5).minimumStock(10).active(true).build();
        Product p3 = Product.builder()
                .id(3L).sku("BEV-003").name("Juice").category(cat)
                .unitPrice(BigDecimal.valueOf(3.00)).stockQuantity(0).minimumStock(5).active(true).build();

        when(productRepository.findInventoryReportList(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(p1, p2, p3));

        com.api.v1.inventory.model.response.InventorySummaryReportResponse summary = reportService.getInventorySummaryReport(
                null, null, null, null, StockStatusFilter.ALL, null
        );

        assertNotNull(summary);
        assertEquals(3, summary.getTotalItems());
        assertEquals(25, summary.getTotalUnits()); // 20 + 5 + 0
        assertEquals(BigDecimal.valueOf(45.00).setScale(2), summary.getTotalInventoryValuation().setScale(2)); // 20*2 + 5*1 + 0*3 = 45
        assertEquals(1, summary.getInStockCount()); // p1 (20 > 10)
        assertEquals(1, summary.getLowStockCount()); // p2 (0 < 5 <= 10)
        assertEquals(1, summary.getOutOfStockCount()); // p3 (0 <= 0)

        assertEquals(1, summary.getCategoryBreakdown().size());
        assertEquals("Beverages", summary.getCategoryBreakdown().get(0).getCategoryName());
        assertEquals(3, summary.getCategoryBreakdown().get(0).getTotalItems());
        assertEquals(25, summary.getCategoryBreakdown().get(0).getTotalUnits());
    }

    @Test
    @DisplayName("Should query stock transactions report with TODAY period")
    void testGetStockTransactionReportToday() {
        StockTransactionResponse item = StockTransactionResponse.builder()
                .id(50L)
                .transactionType(TransactionType.STOCK_IN)
                .quantity(10)
                .build();
        PageResponse<StockTransactionResponse> pageResponse = PageResponse.from(new PageImpl<>(List.of(item)));

        when(stockService.getTransactionHistory(
                eq(1L), eq(TransactionType.STOCK_IN), isNull(), any(Instant.class), any(Instant.class), any(Pageable.class)
        )).thenReturn(pageResponse);

        PageResponse<StockTransactionResponse> result = reportService.getStockTransactionReport(
                1L, TransactionType.STOCK_IN, null, ReportPeriod.TODAY, null, null, "UTC", PageRequest.of(0, 10)
        );

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(stockService).getTransactionHistory(
                eq(1L), eq(TransactionType.STOCK_IN), isNull(), any(Instant.class), any(Instant.class), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("Should export inventory report as Excel")
    void testExportInventoryExcel() {
        when(productRepository.findInventoryReportList(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(testProduct));
        when(excelReportService.generateInventoryReport(anyList(), anyString(), any()))
                .thenReturn(new byte[]{1, 2, 3});

        ExportedFile file = reportService.exportInventoryReport(
                ExportFormat.EXCEL, null, null, null, true, StockStatusFilter.OUT_OF_STOCK, null, "UTC"
        );

        assertNotNull(file);
        assertTrue(file.getFileName().endsWith(".xlsx"));
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file.getContentType());
        assertEquals(3, file.getData().length);
    }

    @Test
    @DisplayName("Should export inventory report as PDF")
    void testExportInventoryPdf() {
        when(productRepository.findInventoryReportList(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(testProduct));
        when(pdfReportService.generateInventoryReport(anyList(), anyString(), any()))
                .thenReturn(new byte[]{4, 5, 6, 7});

        ExportedFile file = reportService.exportInventoryReport(
                ExportFormat.PDF, null, null, null, true, StockStatusFilter.LOW_STOCK, null, "UTC"
        );

        assertNotNull(file);
        assertTrue(file.getFileName().endsWith(".pdf"));
        assertEquals("application/pdf", file.getContentType());
        assertEquals(4, file.getData().length);
    }

    @Test
    @DisplayName("Should export stock transaction report as Excel")
    void testExportStockTransactionsExcel() {
        StockTransaction tx = StockTransaction.builder()
                .id(1L)
                .product(testProduct)
                .transactionType(TransactionType.STOCK_IN)
                .quantity(10)
                .quantityBefore(0)
                .quantityAfter(10)
                .createdAt(Instant.now())
                .build();

        when(stockTransactionRepository.findTransactionsList(any(), any(), any(), any(), any()))
                .thenReturn(List.of(tx));
        when(excelReportService.generateStockTransactionReport(anyList(), anyString(), any()))
                .thenReturn(new byte[]{10, 20});

        ExportedFile file = reportService.exportStockTransactionReport(
                ExportFormat.EXCEL, 1L, TransactionType.STOCK_IN, null, ReportPeriod.THIS_WEEK, null, null, "UTC"
        );

        assertNotNull(file);
        assertTrue(file.getFileName().endsWith(".xlsx"));
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file.getContentType());
    }

    @Test
    @DisplayName("Should export stock transaction report as PDF")
    void testExportStockTransactionsPdf() {
        StockTransaction tx = StockTransaction.builder()
                .id(1L)
                .product(testProduct)
                .transactionType(TransactionType.STOCK_IN)
                .quantity(10)
                .quantityBefore(0)
                .quantityAfter(10)
                .createdAt(Instant.now())
                .build();

        when(stockTransactionRepository.findTransactionsList(any(), any(), any(), any(), any()))
                .thenReturn(List.of(tx));
        when(pdfReportService.generateStockTransactionReport(anyList(), anyString(), any()))
                .thenReturn(new byte[]{30, 40, 50});

        ExportedFile file = reportService.exportStockTransactionReport(
                ExportFormat.PDF, 1L, TransactionType.STOCK_IN, null, ReportPeriod.THIS_MONTH, null, null, "UTC"
        );

        assertNotNull(file);
        assertTrue(file.getFileName().endsWith(".pdf"));
        assertEquals("application/pdf", file.getContentType());
    }
}
