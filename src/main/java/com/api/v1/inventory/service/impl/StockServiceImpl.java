package com.api.v1.inventory.service.impl;

import com.api.v1.inventory.common.exception.BadRequestException;
import com.api.v1.inventory.common.exception.InsufficientStockException;
import com.api.v1.inventory.common.exception.ResourceNotFoundException;
import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.entity.AppUser;
import com.api.v1.inventory.model.entity.Product;
import com.api.v1.inventory.model.entity.StockTransaction;
import com.api.v1.inventory.model.enumerate.TransactionType;
import com.api.v1.inventory.model.request.StockOperationRequest;
import com.api.v1.inventory.model.response.StockTransactionResponse;
import com.api.v1.inventory.repository.ProductRepository;
import com.api.v1.inventory.repository.StockTransactionRepository;
import com.api.v1.inventory.security.CurrentUserService;
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
public class StockServiceImpl implements StockService {

    private final ProductRepository productRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public StockTransactionResponse stockIn(StockOperationRequest request) {
        log.info("Processing Stock In: productId={}, quantity={}", request.getProductId(), request.getQuantity());

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (!product.isActive()) {
            throw new BadRequestException("Cannot perform stock operation on inactive product: " + product.getName());
        }

        int quantityBefore = product.getStockQuantity();
        int quantityAfter = quantityBefore + request.getQuantity();

        product.setStockQuantity(quantityAfter);
        productRepository.save(product);

        AppUser currentUser = currentUserService.getCurrentUser();

        StockTransaction transaction = StockTransaction.builder()
                .product(product)
                .transactionType(TransactionType.STOCK_IN)
                .quantity(request.getQuantity())
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .note(request.getNote() != null ? request.getNote().trim() : null)
                .createdBy(currentUser)
                .build();

        StockTransaction savedTransaction = stockTransactionRepository.save(transaction);

        log.info("Stock In completed successfully: transactionId={}, productId={}, before={}, after={}",
                savedTransaction.getId(), product.getId(), quantityBefore, quantityAfter);

        return StockTransactionResponse.fromEntity(savedTransaction);
    }

    @Override
    @Transactional
    public StockTransactionResponse stockOut(StockOperationRequest request) {
        log.info("Processing Stock Out: productId={}, quantity={}", request.getProductId(), request.getQuantity());

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (!product.isActive()) {
            throw new BadRequestException("Cannot perform stock operation on inactive product: " + product.getName());
        }

        int quantityBefore = product.getStockQuantity();
        if (quantityBefore < request.getQuantity()) {
            throw new InsufficientStockException(String.format(
                    "Insufficient stock for product '%s'. Available: %d, requested: %d",
                    product.getName(), quantityBefore, request.getQuantity()
            ));
        }

        int quantityAfter = quantityBefore - request.getQuantity();

        product.setStockQuantity(quantityAfter);
        productRepository.save(product);

        AppUser currentUser = currentUserService.getCurrentUser();

        StockTransaction transaction = StockTransaction.builder()
                .product(product)
                .transactionType(TransactionType.STOCK_OUT)
                .quantity(request.getQuantity())
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .note(request.getNote() != null ? request.getNote().trim() : null)
                .createdBy(currentUser)
                .build();

        StockTransaction savedTransaction = stockTransactionRepository.save(transaction);

        log.info("Stock Out completed successfully: transactionId={}, productId={}, before={}, after={}",
                savedTransaction.getId(), product.getId(), quantityBefore, quantityAfter);

        return StockTransactionResponse.fromEntity(savedTransaction);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockTransactionResponse> getTransactionHistory(
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

        org.springframework.data.jpa.domain.Specification<StockTransaction> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (productId != null) {
                predicates.add(cb.equal(root.get("product").get("id"), productId));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("transactionType"), type));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("createdBy").get("id"), userId));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<StockTransaction> page = stockTransactionRepository.findAll(spec, pageable);

        return PageResponse.from(page.map(StockTransactionResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public StockTransactionResponse getTransactionById(Long id) {
        StockTransaction transaction = stockTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock transaction not found with id: " + id));

        return StockTransactionResponse.fromEntity(transaction);
    }
}
