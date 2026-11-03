package com.api.v1.inventory.service.report;

import com.api.v1.inventory.model.response.InventoryReportResponse;
import com.api.v1.inventory.model.response.StockTransactionResponse;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PdfReportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("$#,##0.00");
    private static final DecimalFormat INTEGER_FORMAT = new DecimalFormat("#,##0");

    private static final Color HEADER_BG = new Color(31, 78, 121);
    private static final Color SUBHEADER_BG = new Color(70, 130, 180);
    private static final Color ALT_ROW_BG = new Color(245, 247, 250);
    private static final Color BORDER_COLOR = new Color(220, 224, 230);
    private static final Color SUMMARY_BG = new Color(235, 240, 245);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, new Color(31, 78, 121));
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.DARK_GRAY);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(31, 78, 121));
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font SUBHEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
    private static final Font CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
    private static final Font CELL_BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);

    public byte[] generateInventoryReport(List<InventoryReportResponse> items, String filterSummary, ZoneId zoneId) {
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Compute summary aggregations
            int totalQuantity = 0;
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

            // Title Banner
            Paragraph title = new Paragraph("Inventory Status & Valuation Report", TITLE_FONT);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph subtitle = new Paragraph("Filter Criteria: " + filterSummary, SUBTITLE_FONT);
            subtitle.setSpacingAfter(8);
            document.add(subtitle);

            // 1. Stock Summary Report KPI Table
            Paragraph kpiTitle = new Paragraph("Stock Summary Report", SECTION_FONT);
            kpiTitle.setSpacingAfter(4);
            document.add(kpiTitle);

            PdfPTable kpiTable = new PdfPTable(6);
            kpiTable.setWidthPercentage(100);
            kpiTable.setWidths(new float[]{16, 16, 20, 16, 16, 16});

            String[] kpiHeaders = {"Total Items", "Total Units", "Total Valuation", "In Stock", "Low Stock", "Out of Stock"};
            for (String h : kpiHeaders) {
                PdfPCell cell = new PdfPCell(new Phrase(h, SUBHEADER_FONT));
                cell.setBackgroundColor(SUBHEADER_BG);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(4);
                kpiTable.addCell(cell);
            }

            kpiTable.addCell(createCell(INTEGER_FORMAT.format(items.size()), CELL_BOLD_FONT, Element.ALIGN_CENTER, Color.WHITE));
            kpiTable.addCell(createCell(INTEGER_FORMAT.format(totalQuantity), CELL_BOLD_FONT, Element.ALIGN_CENTER, Color.WHITE));
            kpiTable.addCell(createCell(CURRENCY_FORMAT.format(totalInventoryValue), CELL_BOLD_FONT, Element.ALIGN_CENTER, Color.WHITE));
            kpiTable.addCell(createCell(INTEGER_FORMAT.format(inStockCount), CELL_BOLD_FONT, Element.ALIGN_CENTER, Color.WHITE));
            kpiTable.addCell(createCell(INTEGER_FORMAT.format(lowStockCount), CELL_BOLD_FONT, Element.ALIGN_CENTER, Color.WHITE));
            kpiTable.addCell(createCell(INTEGER_FORMAT.format(outOfStockCount), CELL_BOLD_FONT, Element.ALIGN_CENTER, Color.WHITE));

            kpiTable.setSpacingAfter(10);
            document.add(kpiTable);

            // 2. Category Stock Breakdown Table
            Paragraph catTitle = new Paragraph("Category Stock Breakdown", SECTION_FONT);
            catTitle.setSpacingAfter(4);
            document.add(catTitle);

            PdfPTable catTable = new PdfPTable(4);
            catTable.setWidthPercentage(100);
            catTable.setWidths(new float[]{40, 20, 20, 20});

            String[] catHeaders = {"Category Name", "Total Items", "Total Units", "Total Valuation"};
            for (String h : catHeaders) {
                PdfPCell cell = new PdfPCell(new Phrase(h, SUBHEADER_FONT));
                cell.setBackgroundColor(SUBHEADER_BG);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(4);
                catTable.addCell(cell);
            }

            List<CategoryAccumulator> catList = categoryMap.values().stream()
                    .sorted(Comparator.comparing(CategoryAccumulator::getName, String.CASE_INSENSITIVE_ORDER))
                    .toList();

            boolean catAlt = false;
            for (CategoryAccumulator cat : catList) {
                Color bg = catAlt ? ALT_ROW_BG : Color.WHITE;
                catAlt = !catAlt;
                catTable.addCell(createCell(cat.getName(), CELL_FONT, Element.ALIGN_LEFT, bg));
                catTable.addCell(createCell(INTEGER_FORMAT.format(cat.getItems()), CELL_FONT, Element.ALIGN_RIGHT, bg));
                catTable.addCell(createCell(INTEGER_FORMAT.format(cat.getUnits()), CELL_FONT, Element.ALIGN_RIGHT, bg));
                catTable.addCell(createCell(CURRENCY_FORMAT.format(cat.getValuation()), CELL_FONT, Element.ALIGN_RIGHT, bg));
            }

            // Category summary row
            PdfPCell catSumLabel = new PdfPCell(new Phrase("Category Subtotal:", CELL_BOLD_FONT));
            catSumLabel.setBackgroundColor(SUMMARY_BG);
            catSumLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            catSumLabel.setPadding(4);
            catTable.addCell(catSumLabel);

            PdfPCell catSumItems = new PdfPCell(new Phrase(INTEGER_FORMAT.format(items.size()), CELL_BOLD_FONT));
            catSumItems.setBackgroundColor(SUMMARY_BG);
            catSumItems.setHorizontalAlignment(Element.ALIGN_RIGHT);
            catSumItems.setPadding(4);
            catTable.addCell(catSumItems);

            PdfPCell catSumUnits = new PdfPCell(new Phrase(INTEGER_FORMAT.format(totalQuantity), CELL_BOLD_FONT));
            catSumUnits.setBackgroundColor(SUMMARY_BG);
            catSumUnits.setHorizontalAlignment(Element.ALIGN_RIGHT);
            catSumUnits.setPadding(4);
            catTable.addCell(catSumUnits);

            PdfPCell catSumVal = new PdfPCell(new Phrase(CURRENCY_FORMAT.format(totalInventoryValue), CELL_BOLD_FONT));
            catSumVal.setBackgroundColor(SUMMARY_BG);
            catSumVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            catSumVal.setPadding(4);
            catTable.addCell(catSumVal);

            catTable.setSpacingAfter(10);
            document.add(catTable);

            // 3. Detailed Product Table
            Paragraph prodTitle = new Paragraph("Detailed Product Inventory", SECTION_FONT);
            prodTitle.setSpacingAfter(4);
            document.add(prodTitle);

            PdfPTable table = new PdfPTable(10);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{12, 22, 12, 7, 9, 7, 7, 10, 9, 8});

            // Table Headers
            String[] headers = {
                    "SKU", "Product Name", "Category", "Unit", "Unit Price",
                    "Qty", "Min", "Total Value", "Stock Health", "Status"
            };
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, HEADER_FONT));
                cell.setBackgroundColor(HEADER_BG);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(4);
                table.addCell(cell);
            }

            // Data Rows
            boolean alternate = false;

            for (InventoryReportResponse item : items) {
                Color bg = alternate ? ALT_ROW_BG : Color.WHITE;
                alternate = !alternate;

                table.addCell(createCell(item.getSku() != null ? item.getSku() : "", CELL_FONT, Element.ALIGN_LEFT, bg));
                table.addCell(createCell(item.getName() != null ? item.getName() : "", CELL_FONT, Element.ALIGN_LEFT, bg));
                table.addCell(createCell(item.getCategoryName() != null ? item.getCategoryName() : "N/A", CELL_FONT, Element.ALIGN_LEFT, bg));
                table.addCell(createCell(item.getUnitOfMeasure() != null ? item.getUnitOfMeasure().name() : "", CELL_FONT, Element.ALIGN_CENTER, bg));

                BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                table.addCell(createCell(CURRENCY_FORMAT.format(price), CELL_FONT, Element.ALIGN_RIGHT, bg));

                int qty = item.getStockQuantity() != null ? item.getStockQuantity() : 0;
                table.addCell(createCell(INTEGER_FORMAT.format(qty), CELL_FONT, Element.ALIGN_RIGHT, bg));

                int minStock = item.getMinimumStock() != null ? item.getMinimumStock() : 0;
                table.addCell(createCell(INTEGER_FORMAT.format(minStock), CELL_FONT, Element.ALIGN_RIGHT, bg));

                BigDecimal val = item.getTotalValue() != null ? item.getTotalValue() : BigDecimal.ZERO;
                table.addCell(createCell(CURRENCY_FORMAT.format(val), CELL_FONT, Element.ALIGN_RIGHT, bg));

                String health;
                if (qty <= 0) {
                    health = "OUT OF STOCK";
                } else if (item.isLowStock() || qty <= minStock) {
                    health = "LOW STOCK";
                } else {
                    health = "IN STOCK";
                }
                table.addCell(createCell(health, CELL_BOLD_FONT, Element.ALIGN_CENTER, bg));
                table.addCell(createCell(item.isActive() ? "ACTIVE" : "INACTIVE", CELL_FONT, Element.ALIGN_CENTER, bg));
            }

            // Summary row
            PdfPCell sumLabel = new PdfPCell(new Phrase("Total (" + items.size() + " items):", CELL_BOLD_FONT));
            sumLabel.setColspan(5);
            sumLabel.setBackgroundColor(SUMMARY_BG);
            sumLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            sumLabel.setPadding(4);
            table.addCell(sumLabel);

            PdfPCell sumQty = new PdfPCell(new Phrase(INTEGER_FORMAT.format(totalQuantity), CELL_BOLD_FONT));
            sumQty.setBackgroundColor(SUMMARY_BG);
            sumQty.setHorizontalAlignment(Element.ALIGN_RIGHT);
            sumQty.setPadding(4);
            table.addCell(sumQty);

            PdfPCell emptyMin = new PdfPCell(new Phrase("", CELL_BOLD_FONT));
            emptyMin.setBackgroundColor(SUMMARY_BG);
            table.addCell(emptyMin);

            PdfPCell sumVal = new PdfPCell(new Phrase(CURRENCY_FORMAT.format(totalInventoryValue), CELL_BOLD_FONT));
            sumVal.setBackgroundColor(SUMMARY_BG);
            sumVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            sumVal.setPadding(4);
            table.addCell(sumVal);

            PdfPCell emptyRemaining = new PdfPCell(new Phrase("", CELL_BOLD_FONT));
            emptyRemaining.setColspan(2);
            emptyRemaining.setBackgroundColor(SUMMARY_BG);
            table.addCell(emptyRemaining);

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF inventory report", e);
        }
    }

    public byte[] generateStockTransactionReport(List<StockTransactionResponse> items, String filterSummary, ZoneId zoneId) {
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Title Banner
            Paragraph title = new Paragraph("Stock Movement Transactions Report", TITLE_FONT);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph subtitle = new Paragraph("Filter Criteria: " + filterSummary, SUBTITLE_FONT);
            subtitle.setSpacingAfter(12);
            document.add(subtitle);

            // Table
            PdfPTable table = new PdfPTable(11);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{6, 12, 16, 9, 9, 7, 7, 7, 10, 12, 14});

            String[] headers = {
                    "ID", "SKU", "Product Name", "Type", "Reason", "Qty Changed",
                    "Before", "After", "User", "Timestamp", "Note"
            };
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, HEADER_FONT));
                cell.setBackgroundColor(HEADER_BG);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(5);
                table.addCell(cell);
            }

            int totalInQty = 0;
            int totalOutQty = 0;
            boolean alternate = false;

            for (StockTransactionResponse item : items) {
                Color bg = alternate ? ALT_ROW_BG : Color.WHITE;
                alternate = !alternate;

                table.addCell(createCell(String.valueOf(item.getId() != null ? item.getId() : ""), CELL_FONT, Element.ALIGN_CENTER, bg));
                table.addCell(createCell(item.getProductSku() != null ? item.getProductSku() : "", CELL_FONT, Element.ALIGN_LEFT, bg));
                table.addCell(createCell(item.getProductName() != null ? item.getProductName() : "", CELL_FONT, Element.ALIGN_LEFT, bg));

                String type = item.getTransactionType() != null ? item.getTransactionType().name() : "";
                table.addCell(createCell(type, CELL_BOLD_FONT, Element.ALIGN_CENTER, bg));

                String reason = item.getReason() != null ? item.getReason().name() : "";
                table.addCell(createCell(reason, CELL_FONT, Element.ALIGN_CENTER, bg));

                int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                if (type.contains("IN")) {
                    totalInQty += qty;
                } else {
                    totalOutQty += qty;
                }
                table.addCell(createCell(INTEGER_FORMAT.format(qty), CELL_FONT, Element.ALIGN_RIGHT, bg));
                table.addCell(createCell(INTEGER_FORMAT.format(item.getQuantityBefore() != null ? item.getQuantityBefore() : 0), CELL_FONT, Element.ALIGN_RIGHT, bg));
                table.addCell(createCell(INTEGER_FORMAT.format(item.getQuantityAfter() != null ? item.getQuantityAfter() : 0), CELL_FONT, Element.ALIGN_RIGHT, bg));

                table.addCell(createCell(item.getCreatedByUsername() != null ? item.getCreatedByUsername() : "N/A", CELL_FONT, Element.ALIGN_LEFT, bg));

                String dateStr = item.getCreatedAt() != null ? item.getCreatedAt().atZone(zoneId).format(DATE_FORMATTER) : "N/A";
                table.addCell(createCell(dateStr, CELL_FONT, Element.ALIGN_CENTER, bg));
                table.addCell(createCell(item.getNote() != null ? item.getNote() : "", CELL_FONT, Element.ALIGN_LEFT, bg));
            }

            // Summary row
            PdfPCell sumLabel = new PdfPCell(new Phrase("Total Transactions: " + items.size() + " (In: " + totalInQty + ", Out: " + totalOutQty + ")", CELL_BOLD_FONT));
            sumLabel.setColspan(11);
            sumLabel.setBackgroundColor(SUMMARY_BG);
            sumLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
            sumLabel.setPadding(5);
            table.addCell(sumLabel);

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF transaction report", e);
        }
    }

    private PdfPCell createCell(String text, Font font, int alignment, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(bg);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(4);
        return cell;
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
