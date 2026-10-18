package com.api.v1.inventory.service.impl;

import com.api.v1.inventory.common.exception.BadRequestException;
import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.entity.Product;
import com.api.v1.inventory.model.enumerate.TransactionType;
import com.api.v1.inventory.model.response.InventoryReportResponse;
import com.api.v1.inventory.model.response.StockTransactionResponse;
import com.api.v1.inventory.repository.ProductRepository;
import com.api.v1.inventory.service.ReportService;
import com.api.v1.inventory.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final StockService stockService;
    private final ProductRepository productRepository;

    @Override
    public PageResponse<StockTransactionResponse> getStockTransactionReport(
            Long productId,
            TransactionType type,
            Long userId,
            Instant fromDate,
            Instant toDate,
            Pageable pageable
    ) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate must be before or equal to toDate");
        }

        log.debug("Generating stock transaction report: productId={}, type={}, userId={}, fromDate={}, toDate={}",
                productId, type, userId, fromDate, toDate);

        return stockService.getTransactionHistory(productId, type, userId, fromDate, toDate, pageable);
    }

    @Override
    public PageResponse<InventoryReportResponse> getInventoryReport(
            String keyword,
            Long categoryId,
            Long supplierId,
            Boolean active,
            Boolean lowStockOnly,
            Pageable pageable
    ) {
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty())
                ? "%" + keyword.trim().toLowerCase() + "%"
                : null;

        log.debug("Generating inventory report: keyword={}, categoryId={}, supplierId={}, active={}, lowStockOnly={}",
                keyword, categoryId, supplierId, active, lowStockOnly);

        Page<Product> page = productRepository.searchInventoryReport(
                searchKeyword,
                categoryId,
                supplierId,
                active,
                lowStockOnly,
                pageable
        );

        return PageResponse.from(page.map(InventoryReportResponse::fromEntity));
    }
}
