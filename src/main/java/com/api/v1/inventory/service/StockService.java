package com.api.v1.inventory.service;

import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.enumerate.TransactionType;
import com.api.v1.inventory.model.request.StockOperationRequest;
import com.api.v1.inventory.model.response.StockTransactionResponse;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface StockService {

    StockTransactionResponse stockIn(StockOperationRequest request);

    StockTransactionResponse stockOut(StockOperationRequest request);

    PageResponse<StockTransactionResponse> getTransactionHistory(
            Long productId,
            TransactionType type,
            Long userId,
            Instant fromDate,
            Instant toDate,
            Pageable pageable
    );

    StockTransactionResponse getTransactionById(Long id);
}
