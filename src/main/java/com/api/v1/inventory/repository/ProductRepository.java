package com.api.v1.inventory.repository;

import com.api.v1.inventory.model.entity.Product;
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

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @EntityGraph(attributePaths = {"category", "supplier"})
    Optional<Product> findBySkuIgnoreCase(String sku);

    @Override
    @EntityGraph(attributePaths = {"category", "supplier"})
    Optional<Product> findById(Long id);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    boolean existsByCategoryId(Long categoryId);

    @Query("SELECT DISTINCT p.category.id FROM Product p WHERE p.category.id IN :categoryIds")
    java.util.List<Long> findReferencedCategoryIds(@Param("categoryIds") java.util.Collection<Long> categoryIds);

    boolean existsBySupplierId(Long supplierId);

    @Query("SELECT DISTINCT p.supplier.id FROM Product p WHERE p.supplier.id IN :supplierIds")
    java.util.List<Long> findReferencedSupplierIds(@Param("supplierIds") java.util.Collection<Long> supplierIds);

    @EntityGraph(attributePaths = {"category", "supplier"})
    @Query("SELECT p FROM Product p " +
           "WHERE (CAST(:keyword AS string) IS NULL " +
           "       OR LOWER(p.name) LIKE CAST(:keyword AS string) " +
           "       OR LOWER(p.sku) LIKE CAST(:keyword AS string)) " +
           "  AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "  AND (:supplierId IS NULL OR p.supplier.id = :supplierId) " +
           "  AND (:active IS NULL OR p.active = :active) " +
           "  AND (" +
           "       (:stockStatus IS NULL OR :stockStatus = 'ALL') " +
           "       OR (:stockStatus = 'IN_STOCK' AND p.stockQuantity > p.minimumStock) " +
           "       OR (:stockStatus = 'LOW_STOCK' AND p.stockQuantity <= p.minimumStock AND p.stockQuantity > 0) " +
           "       OR (:stockStatus = 'OUT_OF_STOCK' AND p.stockQuantity <= 0)" +
           "  ) " +
           "  AND (:lowStockOnly IS NULL OR :lowStockOnly = false OR p.stockQuantity <= p.minimumStock)")
    Page<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("supplierId") Long supplierId,
            @Param("active") Boolean active,
            @Param("stockStatus") String stockStatus,
            @Param("lowStockOnly") Boolean lowStockOnly,
            Pageable pageable);

    default Page<Product> searchProducts(
            String keyword,
            Long categoryId,
            Long supplierId,
            Boolean active,
            Pageable pageable) {
        return searchProducts(keyword, categoryId, supplierId, active, "ALL", null, pageable);
    }

    @EntityGraph(attributePaths = {"category", "supplier"})
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stockQuantity <= p.minimumStock")
    Page<Product> findLowStockProducts(Pageable pageable);

    long countByActiveTrue();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true AND p.stockQuantity <= p.minimumStock AND p.stockQuantity > 0")
    long countLowStockProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true AND p.stockQuantity = 0")
    long countOutOfStockProducts();

    @Query("SELECT COALESCE(SUM(p.stockQuantity), 0) FROM Product p WHERE p.active = true AND p.stockQuantity > 0")
    long sumTotalStockQuantity();

    @Query("SELECT COALESCE(SUM(p.unitPrice * p.stockQuantity), 0) FROM Product p WHERE p.active = true AND p.stockQuantity > 0")
    java.math.BigDecimal sumTotalInventoryValue();

    @Override
    @EntityGraph(attributePaths = {"category", "supplier"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"category", "supplier"})
    java.util.List<Product> findAll(Specification<Product> spec, Sort sort);

    default Page<Product> searchInventoryReport(
            String keyword,
            Long categoryId,
            Long supplierId,
            Boolean active,
            String stockStatus,
            Boolean lowStockOnly,
            java.time.Instant fromDate,
            java.time.Instant toDate,
            Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("sku")), pattern)
                ));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (supplierId != null) {
                predicates.add(cb.equal(root.get("supplier").get("id"), supplierId));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (stockStatus != null && !"ALL".equalsIgnoreCase(stockStatus)) {
                if ("IN_STOCK".equalsIgnoreCase(stockStatus)) {
                    predicates.add(cb.greaterThan(root.get("stockQuantity"), root.get("minimumStock")));
                } else if ("LOW_STOCK".equalsIgnoreCase(stockStatus)) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("stockQuantity"), root.get("minimumStock")));
                    predicates.add(cb.greaterThan(root.get("stockQuantity"), 0));
                } else if ("OUT_OF_STOCK".equalsIgnoreCase(stockStatus)) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("stockQuantity"), 0));
                }
            }
            if (Boolean.TRUE.equals(lowStockOnly)) {
                predicates.add(cb.lessThanOrEqualTo(root.get("stockQuantity"), root.get("minimumStock")));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("updatedAt"), toDate));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return findAll(spec, pageable);
    }

    default Page<Product> searchInventoryReport(
            String keyword,
            Long categoryId,
            Long supplierId,
            Boolean active,
            String stockStatus,
            Boolean lowStockOnly,
            Pageable pageable) {
        return searchInventoryReport(keyword, categoryId, supplierId, active, stockStatus, lowStockOnly, null, null, pageable);
    }

    default java.util.List<Product> findInventoryReportList(
            String keyword,
            Long categoryId,
            Long supplierId,
            Boolean active,
            String stockStatus,
            Boolean lowStockOnly,
            java.time.Instant fromDate,
            java.time.Instant toDate) {
        Specification<Product> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("sku")), pattern)
                ));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (supplierId != null) {
                predicates.add(cb.equal(root.get("supplier").get("id"), supplierId));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (stockStatus != null && !"ALL".equalsIgnoreCase(stockStatus)) {
                if ("IN_STOCK".equalsIgnoreCase(stockStatus)) {
                    predicates.add(cb.greaterThan(root.get("stockQuantity"), root.get("minimumStock")));
                } else if ("LOW_STOCK".equalsIgnoreCase(stockStatus)) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("stockQuantity"), root.get("minimumStock")));
                    predicates.add(cb.greaterThan(root.get("stockQuantity"), 0));
                } else if ("OUT_OF_STOCK".equalsIgnoreCase(stockStatus)) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("stockQuantity"), 0));
                }
            }
            if (Boolean.TRUE.equals(lowStockOnly)) {
                predicates.add(cb.lessThanOrEqualTo(root.get("stockQuantity"), root.get("minimumStock")));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("updatedAt"), toDate));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return findAll(spec, Sort.by(Sort.Direction.ASC, "id"));
    }

    default java.util.List<Product> findInventoryReportList(
            String keyword,
            Long categoryId,
            Long supplierId,
            Boolean active,
            String stockStatus,
            Boolean lowStockOnly) {
        return findInventoryReportList(keyword, categoryId, supplierId, active, stockStatus, lowStockOnly, null, null);
    }
}
