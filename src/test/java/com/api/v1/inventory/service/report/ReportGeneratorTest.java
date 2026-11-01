package com.api.v1.inventory.service.report;

import com.api.v1.inventory.model.enumerate.TransactionType;
import com.api.v1.inventory.model.enumerate.UnitOfMeasure;
import com.api.v1.inventory.model.response.InventoryReportResponse;
import com.api.v1.inventory.model.response.StockTransactionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportGeneratorTest {

    private final ExcelReportService excelReportService = new ExcelReportService();
    private final PdfReportService pdfReportService = new PdfReportService();

    @Test
    @DisplayName("Should generate Excel inventory report successfully")
    void testExcelInventoryReport() {
        InventoryReportResponse item1 = InventoryReportResponse.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Keyboard")
                .categoryName("Electronics")
                .supplierName("Logitech")
                .unitOfMeasure(UnitOfMeasure.PCS)
                .unitPrice(BigDecimal.valueOf(45.50))
                .stockQuantity(10)
                .minimumStock(5)
                .totalValue(BigDecimal.valueOf(455.00))
                .lowStock(false)
                .active(true)
                .updatedAt(Instant.now())
                .build();

        InventoryReportResponse item2 = InventoryReportResponse.builder()
                .id(2L)
                .sku("SKU-002")
                .name("Mouse")
                .categoryName("Electronics")
                .supplierName("Logitech")
                .unitOfMeasure(UnitOfMeasure.PCS)
                .unitPrice(BigDecimal.valueOf(25.00))
                .stockQuantity(0)
                .minimumStock(10)
                .totalValue(BigDecimal.ZERO)
                .lowStock(true)
                .active(true)
                .updatedAt(Instant.now())
                .build();

        byte[] bytes = excelReportService.generateInventoryReport(List.of(item1, item2), "All Products", ZoneId.of("UTC"));
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    @DisplayName("Should generate PDF inventory report successfully")
    void testPdfInventoryReport() {
        InventoryReportResponse item = InventoryReportResponse.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Keyboard")
                .categoryName("Electronics")
                .supplierName("Logitech")
                .unitOfMeasure(UnitOfMeasure.PCS)
                .unitPrice(BigDecimal.valueOf(45.50))
                .stockQuantity(3)
                .minimumStock(5)
                .totalValue(BigDecimal.valueOf(136.50))
                .lowStock(true)
                .active(true)
                .updatedAt(Instant.now())
                .build();

        byte[] bytes = pdfReportService.generateInventoryReport(List.of(item), "Low Stock Products", ZoneId.of("UTC"));
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    @DisplayName("Should generate Excel and PDF stock transactions report successfully")
    void testStockTransactionReports() {
        StockTransactionResponse tx = StockTransactionResponse.builder()
                .id(101L)
                .productId(1L)
                .productSku("SKU-001")
                .productName("Keyboard")
                .transactionType(TransactionType.STOCK_IN)
                .unitOfMeasure(UnitOfMeasure.PCS)
                .quantity(15)
                .quantityBefore(5)
                .quantityAfter(20)
                .note("Received from supplier")
                .createdById(2L)
                .createdByUsername("admin")
                .createdAt(Instant.now())
                .build();

        byte[] excelBytes = excelReportService.generateStockTransactionReport(List.of(tx), "Today", ZoneId.of("UTC"));
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);

        byte[] pdfBytes = pdfReportService.generateStockTransactionReport(List.of(tx), "Today", ZoneId.of("UTC"));
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}
