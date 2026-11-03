package com.api.v1.inventory.repository;

import com.api.v1.inventory.model.entity.StockTransaction;
import com.api.v1.inventory.model.enumerate.StockOutReason;
import com.api.v1.inventory.model.enumerate.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long>, JpaSpecificationExecutor<StockTransaction> {

    @Override
    @EntityGraph(attributePaths = {"product", "createdBy", "supplier", "customer"})
    Optional<StockTransaction> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"product", "createdBy", "supplier", "customer"})
    Page<StockTransaction> findAll(Specification<StockTransaction> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"product", "createdBy", "supplier", "customer"})
    java.util.List<StockTransaction> findAll(Specification<StockTransaction> spec, Sort sort);

    boolean existsByProductId(Long productId);

    @Query("SELECT DISTINCT t.product.id FROM StockTransaction t WHERE t.product.id IN :productIds")
    java.util.List<Long> findReferencedProductIds(@Param("productIds") java.util.Collection<Long> productIds);

    boolean existsByCreatedById(Long userId);

    @Query("SELECT DISTINCT t.createdBy.id FROM StockTransaction t WHERE t.createdBy.id IN :userIds")
    java.util.List<Long> findReferencedUserIds(@Param("userIds") java.util.Collection<Long> userIds);

    boolean existsBySupplierId(Long supplierId);

    @Query("SELECT DISTINCT t.supplier.id FROM StockTransaction t WHERE t.supplier.id IN :supplierIds")
    java.util.List<Long> findReferencedSupplierIds(@Param("supplierIds") java.util.Collection<Long> supplierIds);

    boolean existsByCustomerId(Long customerId);

    @Query("SELECT DISTINCT t.customer.id FROM StockTransaction t WHERE t.customer.id IN :customerIds")
    java.util.List<Long> findReferencedCustomerIds(@Param("customerIds") java.util.Collection<Long> customerIds);

    @EntityGraph(attributePaths = {"product", "createdBy", "supplier", "customer"})
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

    default java.util.List<StockTransaction> findTransactionsList(
            Long productId,
            TransactionType type,
            StockOutReason reason,
            Long userId,
            Instant fromDate,
            Instant toDate) {
        Specification<StockTransaction> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (productId != null) {
                predicates.add(cb.equal(root.get("product").get("id"), productId));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("transactionType"), type));
            }
            if (reason != null) {
                predicates.add(cb.equal(root.get("reason"), reason));
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
        return findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM StockTransaction t WHERE t.transactionType = :type")
    long sumQuantityByType(@Param("type") TransactionType type);

    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM StockTransaction t " +
           "WHERE t.transactionType = :type " +
           "  AND (cast(:fromDate as timestamp) IS NULL OR t.createdAt >= :fromDate) " +
           "  AND (cast(:toDate as timestamp) IS NULL OR t.createdAt <= :toDate)")
    long sumQuantityByTypeAndDateRange(
            @Param("type") TransactionType type,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate);

    @Query("SELECT COALESCE(SUM(t.quantity * p.unitPrice), 0.0) FROM StockTransaction t " +
           "JOIN t.product p " +
           "WHERE t.transactionType = :type " +
           "  AND (cast(:fromDate as timestamp) IS NULL OR t.createdAt >= :fromDate) " +
           "  AND (cast(:toDate as timestamp) IS NULL OR t.createdAt <= :toDate)")
    BigDecimal sumValueByTypeAndDateRange(
            @Param("type") TransactionType type,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate);

    @Query("SELECT COUNT(DISTINCT t.product.id) FROM StockTransaction t " +
           "WHERE t.transactionType = :type " +
           "  AND (cast(:fromDate as timestamp) IS NULL OR t.createdAt >= :fromDate) " +
           "  AND (cast(:toDate as timestamp) IS NULL OR t.createdAt <= :toDate)")
    long countDistinctProductsByTypeAndDateRange(
            @Param("type") TransactionType type,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate);
}
