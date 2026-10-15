package com.api.v1.inventory.repository;

import com.api.v1.inventory.model.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"category", "supplier"})
    Optional<Product> findBySkuIgnoreCase(String sku);

    @Override
    @EntityGraph(attributePaths = {"category", "supplier"})
    Optional<Product> findById(Long id);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    boolean existsByCategoryId(Long categoryId);

    boolean existsBySupplierId(Long supplierId);

    @EntityGraph(attributePaths = {"category", "supplier"})
    @Query("SELECT p FROM Product p " +
           "WHERE (CAST(:keyword AS string) IS NULL " +
           "       OR LOWER(p.name) LIKE CAST(:keyword AS string) " +
           "       OR LOWER(p.sku) LIKE CAST(:keyword AS string)) " +
           "  AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "  AND (:supplierId IS NULL OR p.supplier.id = :supplierId) " +
           "  AND (:active IS NULL OR p.active = :active)")
    Page<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("supplierId") Long supplierId,
            @Param("active") Boolean active,
            Pageable pageable);

    @EntityGraph(attributePaths = {"category", "supplier"})
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stockQuantity <= p.minimumStock")
    Page<Product> findLowStockProducts(Pageable pageable);

    long countByActiveTrue();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true AND p.stockQuantity <= p.minimumStock")
    long countLowStockProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true AND p.stockQuantity = 0")
    long countOutOfStockProducts();
}
