package com.api.v1.inventory.repository;

import com.api.v1.inventory.model.entity.StockTransaction;
import com.api.v1.inventory.model.enumerate.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long>, JpaSpecificationExecutor<StockTransaction> {

    boolean existsByProductId(Long productId);

    @Query("SELECT t FROM StockTransaction t " +
           "WHERE (:productId IS NULL OR t.product.id = :productId) " +
           "  AND (:type IS NULL OR t.transactionType = :type) " +
           "  AND (:userId IS NULL OR t.createdBy.id = :userId) " +
           "  AND (:fromDate IS NULL OR t.createdAt >= :fromDate) " +
           "  AND (:toDate IS NULL OR t.createdAt <= :toDate)")
    Page<StockTransaction> filterTransactions(
            @Param("productId") Long productId,
            @Param("type") TransactionType type,
            @Param("userId") Long userId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM StockTransaction t WHERE t.transactionType = :type")
    long sumQuantityByType(@Param("type") TransactionType type);
}
