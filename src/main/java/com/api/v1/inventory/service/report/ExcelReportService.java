package com.api.v1.inventory.service.report;

import com.api.v1.inventory.model.response.InventoryReportResponse;
import com.api.v1.inventory.model.response.StockTransactionResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelReportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] generateInventoryReport(List<InventoryReportResponse> items, String filterSummary, ZoneId zoneId) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Inventory Report");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle subheaderStyle = createSubheaderStyle(workbook);
            CellStyle sectionTitleStyle = createSectionTitleStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle subtitleStyle = createSubtitleStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle integerStyle = createIntegerStyle(workbook);
            CellStyle centerStyle = createCenterStyle(workbook);
            CellStyle summaryStyle = createSummaryStyle(workbook);
            CellStyle summaryCurrencyStyle = createSummaryCurrencyStyle(workbook);
            CellStyle summaryIntegerStyle = createSummaryIntegerStyle(workbook);

            // Calculate aggregations
            long totalQuantity = 0;
            BigDecimal totalInventoryValue = BigDecimal.ZERO;
            long inStockCount = 0;
            long lowStockCount = 0;
            long outOfStockCount = 0;

            Map<String, CategoryAccumulator> categoryMap = new LinkedHashMap<>();

            for (InventoryReportResponse item : items) {
                int qty = item.getStockQuantity() != null ? item.getStockQuantity() : 0;
                int min = item.getMinimumStock() != null ? item.getMinimumStock() : 0;
                BigDecimal val = item.getTotalValue() != null ? item.getTotalValue() : BigDecimal.ZERO;

                totalQuantity += qty;
                totalInventoryValue = totalInventoryValue.add(val);

                if (qty <= 0) {
                    outOfStockCount++;
                } else if (item.isLowStock() || qty <= min) {
                    lowStockCount++;
                } else {
                    inStockCount++;
                }

                String catName = item.getCategoryName() != null ? item.getCategoryName() : "Uncategorized";
                categoryMap.computeIfAbsent(catName, CategoryAccumulator::new).addItem(qty, val);
            }

            // Title Row
            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Inventory Status & Valuation Report");
            titleCell.setCellStyle(titleStyle);

            // Subtitle Row
            Row subtitleRow = sheet.createRow(rowIdx++);
            Cell subtitleCell = subtitleRow.createCell(0);
            subtitleCell.setCellValue("Filter Criteria: " + filterSummary);
            subtitleCell.setCellStyle(subtitleStyle);

            rowIdx++; // spacing

            // 1. Stock Summary Report Section
            Row kpiTitleRow = sheet.createRow(rowIdx++);
            Cell kpiTitleCell = kpiTitleRow.createCell(0);
            kpiTitleCell.setCellValue("STOCK SUMMARY REPORT");
            kpiTitleCell.setCellStyle(sectionTitleStyle);

            Row kpiHeaderRow = sheet.createRow(rowIdx++);
            String[] kpiHeaders = {"Total Items", "Total Units", "Total Valuation", "In Stock", "Low Stock", "Out of Stock"};
            for (int i = 0; i < kpiHeaders.length; i++) {
                Cell cell = kpiHeaderRow.createCell(i);
                cell.setCellValue(kpiHeaders[i]);
                cell.setCellStyle(subheaderStyle);
            }

            Row kpiDataRow = sheet.createRow(rowIdx++);
            Cell kpi0 = kpiDataRow.createCell(0); kpi0.setCellValue(items.size()); kpi0.setCellStyle(summaryIntegerStyle);
            Cell kpi1 = kpiDataRow.createCell(1); kpi1.setCellValue(totalQuantity); kpi1.setCellStyle(summaryIntegerStyle);
            Cell kpi2 = kpiDataRow.createCell(2); kpi2.setCellValue(totalInventoryValue.doubleValue()); kpi2.setCellStyle(summaryCurrencyStyle);
            Cell kpi3 = kpiDataRow.createCell(3); kpi3.setCellValue(inStockCount); kpi3.setCellStyle(summaryIntegerStyle);
            Cell kpi4 = kpiDataRow.createCell(4); kpi4.setCellValue(lowStockCount); kpi4.setCellStyle(summaryIntegerStyle);
            Cell kpi5 = kpiDataRow.createCell(5); kpi5.setCellValue(outOfStockCount); kpi5.setCellStyle(summaryIntegerStyle);

            rowIdx++; // spacing

            // 2. Category Stock Breakdown Section
            Row catTitleRow = sheet.createRow(rowIdx++);
            Cell catTitleCell = catTitleRow.createCell(0);
            catTitleCell.setCellValue("CATEGORY STOCK BREAKDOWN");
            catTitleCell.setCellStyle(sectionTitleStyle);

            Row catHeaderRow = sheet.createRow(rowIdx++);
            String[] catHeaders = {"Category Name", "Total Items", "Total Units", "Total Valuation"};
            for (int i = 0; i < catHeaders.length; i++) {
                Cell cell = catHeaderRow.createCell(i);
                cell.setCellValue(catHeaders[i]);
                cell.setCellStyle(subheaderStyle);
            }

            List<CategoryAccumulator> catList = categoryMap.values().stream()
                    .sorted(Comparator.comparing(CategoryAccumulator::getName, String.CASE_INSENSITIVE_ORDER))
                    .toList();

            for (CategoryAccumulator cat : catList) {
                Row catRow = sheet.createRow(rowIdx++);
                catRow.createCell(0).setCellValue(cat.getName());
                Cell cItems = catRow.createCell(1); cItems.setCellValue(cat.getItems()); cItems.setCellStyle(integerStyle);
                Cell cUnits = catRow.createCell(2); cUnits.setCellValue(cat.getUnits()); cUnits.setCellStyle(integerStyle);
                Cell cVal = catRow.createCell(3); cVal.setCellValue(cat.getValuation().doubleValue()); cVal.setCellStyle(currencyStyle);
            }

            Row catSummaryRow = sheet.createRow(rowIdx++);
            Cell cs0 = catSummaryRow.createCell(0); cs0.setCellValue("Category Total / Subtotal:"); cs0.setCellStyle(summaryStyle);
            Cell cs1 = catSummaryRow.createCell(1); cs1.setCellValue(items.size()); cs1.setCellStyle(summaryIntegerStyle);
            Cell cs2 = catSummaryRow.createCell(2); cs2.setCellValue(totalQuantity); cs2.setCellStyle(summaryIntegerStyle);
            Cell cs3 = catSummaryRow.createCell(3); cs3.setCellValue(totalInventoryValue.doubleValue()); cs3.setCellStyle(summaryCurrencyStyle);

            rowIdx++; // spacing

            // 3. Product Inventory Details Section
            Row prodTitleRow = sheet.createRow(rowIdx++);
            Cell prodTitleCell = prodTitleRow.createCell(0);
            prodTitleCell.setCellValue("DETAILED PRODUCT INVENTORY");
            prodTitleCell.setCellStyle(sectionTitleStyle);

            // Table Header Row
            String[] columns = {
                    "ID", "SKU", "Product Name", "Category", "Supplier", "Unit",
                    "Unit Price", "Stock Qty", "Min Stock", "Total Value",
                    "Stock Health", "Status", "Last Updated"
            };

            Row headerRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            for (InventoryReportResponse item : items) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(item.getId() != null ? item.getId() : 0);
                row.getCell(0).setCellStyle(integerStyle);

                row.createCell(1).setCellValue(item.getSku() != null ? item.getSku() : "");
                row.createCell(2).setCellValue(item.getName() != null ? item.getName() : "");
                row.createCell(3).setCellValue(item.getCategoryName() != null ? item.getCategoryName() : "N/A");
                row.createCell(4).setCellValue(item.getSupplierName() != null ? item.getSupplierName() : "N/A");

                Cell unitCell = row.createCell(5);
                unitCell.setCellValue(item.getUnitOfMeasure() != null ? item.getUnitOfMeasure().name() : "");
                unitCell.setCellStyle(centerStyle);

                Cell priceCell = row.createCell(6);
                priceCell.setCellValue(item.getUnitPrice() != null ? item.getUnitPrice().doubleValue() : 0.0);
                priceCell.setCellStyle(currencyStyle);

                int qty = item.getStockQuantity() != null ? item.getStockQuantity() : 0;
                Cell qtyCell = row.createCell(7);
                qtyCell.setCellValue(qty);
                qtyCell.setCellStyle(integerStyle);

                Cell minStockCell = row.createCell(8);
                minStockCell.setCellValue(item.getMinimumStock() != null ? item.getMinimumStock() : 0);
                minStockCell.setCellStyle(integerStyle);

                BigDecimal itemTotalVal = item.getTotalValue() != null ? item.getTotalValue() : BigDecimal.ZERO;
                Cell totalValCell = row.createCell(9);
                totalValCell.setCellValue(itemTotalVal.doubleValue());
                totalValCell.setCellStyle(currencyStyle);

                // Stock Health
                String health;
                if (qty <= 0) {
                    health = "OUT OF STOCK";
                } else if (item.isLowStock() || qty <= (item.getMinimumStock() != null ? item.getMinimumStock() : 0)) {
                    health = "LOW STOCK";
                } else {
                    health = "IN STOCK";
                }
                Cell healthCell = row.createCell(10);
                healthCell.setCellValue(health);
                healthCell.setCellStyle(centerStyle);

                Cell statusCell = row.createCell(11);
                statusCell.setCellValue(item.isActive() ? "ACTIVE" : "INACTIVE");
                statusCell.setCellStyle(centerStyle);

                Cell dateCell = row.createCell(12);
                if (item.getUpdatedAt() != null) {
                    dateCell.setCellValue(item.getUpdatedAt().atZone(zoneId).format(DATE_FORMATTER));
                } else {
                    dateCell.setCellValue("N/A");
                }
                dateCell.setCellStyle(centerStyle);
            }

            // Summary Row
            Row summaryRow = sheet.createRow(rowIdx);
            Cell sumLabelCell = summaryRow.createCell(2);
            sumLabelCell.setCellValue("Total / Summary (" + items.size() + " products):");
            sumLabelCell.setCellStyle(summaryStyle);

            Cell sumQtyCell = summaryRow.createCell(7);
            sumQtyCell.setCellValue(totalQuantity);
            sumQtyCell.setCellStyle(summaryIntegerStyle);

            Cell sumValCell = summaryRow.createCell(9);
            sumValCell.setCellValue(totalInventoryValue.doubleValue());
            sumValCell.setCellStyle(summaryCurrencyStyle);

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 3200) {
                    sheet.setColumnWidth(i, 3500);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel inventory report", e);
        }
    }

    public byte[] generateStockTransactionReport(List<StockTransactionResponse> items, String filterSummary, ZoneId zoneId) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Stock Transactions");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle subtitleStyle = createSubtitleStyle(workbook);
            CellStyle integerStyle = createIntegerStyle(workbook);
            CellStyle centerStyle = createCenterStyle(workbook);
            CellStyle summaryStyle = createSummaryStyle(workbook);
            CellStyle summaryIntegerStyle = createSummaryIntegerStyle(workbook);

            // Title Row
            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Stock Movement Transactions Report");
            titleCell.setCellStyle(titleStyle);

            // Subtitle Row
            Row subtitleRow = sheet.createRow(rowIdx++);
            Cell subtitleCell = subtitleRow.createCell(0);
            subtitleCell.setCellValue("Filter Criteria: " + filterSummary);
            subtitleCell.setCellStyle(subtitleStyle);

            rowIdx++; // spacing

            // Headers
            String[] columns = {
                    "Transaction ID", "SKU", "Product Name", "Type", "Unit",
                    "Qty Changed", "Qty Before", "Qty After", "Performed By",
                    "Timestamp", "Note"
            };

            Row headerRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int totalInQty = 0;
            int totalOutQty = 0;

            for (StockTransactionResponse item : items) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(item.getId() != null ? item.getId() : 0);
                row.getCell(0).setCellStyle(integerStyle);

                row.createCell(1).setCellValue(item.getProductSku() != null ? item.getProductSku() : "");
                row.createCell(2).setCellValue(item.getProductName() != null ? item.getProductName() : "");

                Cell typeCell = row.createCell(3);
                typeCell.setCellValue(item.getTransactionType() != null ? item.getTransactionType().name() : "");
                typeCell.setCellStyle(centerStyle);

                Cell unitCell = row.createCell(4);
                unitCell.setCellValue(item.getUnitOfMeasure() != null ? item.getUnitOfMeasure().name() : "");
                unitCell.setCellStyle(centerStyle);

                int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                if (item.getTransactionType() != null) {
                    if (item.getTransactionType().name().contains("IN")) {
                        totalInQty += qty;
                    } else {
                        totalOutQty += qty;
                    }
                }
                Cell qtyCell = row.createCell(5);
                qtyCell.setCellValue(qty);
                qtyCell.setCellStyle(integerStyle);

                Cell beforeCell = row.createCell(6);
                beforeCell.setCellValue(item.getQuantityBefore() != null ? item.getQuantityBefore() : 0);
                beforeCell.setCellStyle(integerStyle);

                Cell afterCell = row.createCell(7);
                afterCell.setCellValue(item.getQuantityAfter() != null ? item.getQuantityAfter() : 0);
                afterCell.setCellStyle(integerStyle);

                row.createCell(8).setCellValue(item.getCreatedByUsername() != null ? item.getCreatedByUsername() : "N/A");

                Cell dateCell = row.createCell(9);
                if (item.getCreatedAt() != null) {
                    dateCell.setCellValue(item.getCreatedAt().atZone(zoneId).format(DATE_FORMATTER));
                } else {
                    dateCell.setCellValue("N/A");
                }
                dateCell.setCellStyle(centerStyle);

                row.createCell(10).setCellValue(item.getNote() != null ? item.getNote() : "");
            }

            // Summary
            Row summaryRow = sheet.createRow(rowIdx);
            Cell sumLabelCell = summaryRow.createCell(2);
            sumLabelCell.setCellValue("Total Transactions: " + items.size() + " (Total In: " + totalInQty + ", Total Out: " + totalOutQty + ")");
            sumLabelCell.setCellStyle(summaryStyle);

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 3000) {
                    sheet.setColumnWidth(i, 3500);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel transaction report", e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle createSubtitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("$#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createIntegerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createCenterStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createSummaryStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.DOUBLE);
        return style;
    }

    private CellStyle createSummaryCurrencyStyle(Workbook workbook) {
        CellStyle style = createCurrencyStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.DOUBLE);
        return style;
    }

    private CellStyle createSummaryIntegerStyle(Workbook workbook) {
        CellStyle style = createIntegerStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.DOUBLE);
        return style;
    }

    private CellStyle createSubheaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createSectionTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        return style;
    }

    private static class CategoryAccumulator {
        private final String name;
        private long items = 0;
        private long units = 0;
        private BigDecimal valuation = BigDecimal.ZERO;

        public CategoryAccumulator(String name) {
            this.name = name;
        }

        public void addItem(int qty, BigDecimal val) {
            this.items++;
            this.units += qty;
            this.valuation = this.valuation.add(val);
        }

        public String getName() { return name; }
        public long getItems() { return items; }
        public long getUnits() { return units; }
        public BigDecimal getValuation() { return valuation; }
    }
}
