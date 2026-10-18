package com.api.v1.inventory.service.impl;

import com.api.v1.inventory.common.exception.ConflictException;
import com.api.v1.inventory.common.exception.ResourceNotFoundException;
import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.entity.Category;
import com.api.v1.inventory.model.entity.Product;
import com.api.v1.inventory.model.entity.Supplier;
import com.api.v1.inventory.model.request.CreateProductRequest;
import com.api.v1.inventory.model.request.UpdateProductRequest;
import com.api.v1.inventory.model.response.ProductResponse;
import com.api.v1.inventory.repository.CategoryRepository;
import com.api.v1.inventory.repository.ProductRepository;
import com.api.v1.inventory.repository.StockTransactionRepository;
import com.api.v1.inventory.repository.SupplierRepository;
import com.api.v1.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final StockTransactionRepository stockTransactionRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        String trimmedSku = request.getSku().trim();
        if (productRepository.existsBySkuIgnoreCase(trimmedSku)) {
            throw new ConflictException("Product with SKU '" + trimmedSku + "' already exists");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        Supplier supplier = null;
        if (request.getSupplierId() != null) {
            supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.getSupplierId()));
        }

        com.api.v1.inventory.model.enumerate.UnitOfMeasure uom = request.getUnitOfMeasure() != null
                ? request.getUnitOfMeasure()
                : com.api.v1.inventory.model.enumerate.UnitOfMeasure.PCS;

        Product product = Product.builder()
                .sku(trimmedSku)
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .category(category)
                .supplier(supplier)
                .unitOfMeasure(uom)
                .unitPrice(request.getUnitPrice())
                .stockQuantity(0)
                .minimumStock(request.getMinimumStock() != null ? request.getMinimumStock() : 0)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Product saved = productRepository.save(product);
        log.info("Product created successfully with id: {}, SKU: {}", saved.getId(), saved.getSku());
        return ProductResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(
            String keyword,
            Long categoryId,
            Long supplierId,
            Boolean active,
            Pageable pageable
    ) {
        String searchPattern = (keyword != null && !keyword.trim().isEmpty())
                ? "%" + keyword.trim().toLowerCase() + "%"
                : null;
        Page<Product> page = productRepository.searchProducts(
                searchPattern,
                categoryId,
                supplierId,
                active,
                pageable
        );
        return PageResponse.from(page.map(ProductResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return ProductResponse.fromEntity(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        Product product = productRepository.findBySkuIgnoreCase(sku.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku.trim()));
        return ProductResponse.fromEntity(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        String trimmedSku = request.getSku().trim();
        if (!product.getSku().equalsIgnoreCase(trimmedSku) &&
                productRepository.existsBySkuIgnoreCaseAndIdNot(trimmedSku, id)) {
            throw new ConflictException("Product with SKU '" + trimmedSku + "' already exists");
        }

        if (!product.getCategory().getId().equals(request.getCategoryId())) {
            Category newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
            product.setCategory(newCategory);
        }

        if (request.getSupplierId() != null) {
            if (product.getSupplier() == null || !product.getSupplier().getId().equals(request.getSupplierId())) {
                Supplier newSupplier = supplierRepository.findById(request.getSupplierId())
                        .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.getSupplierId()));
                product.setSupplier(newSupplier);
            }
        } else {
            product.setSupplier(null);
        }

        product.setSku(trimmedSku);
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        product.setUnitPrice(request.getUnitPrice());
        product.setMinimumStock(request.getMinimumStock());
        product.setActive(request.getActive());
        if (request.getUnitOfMeasure() != null) {
            product.setUnitOfMeasure(request.getUnitOfMeasure());
        }
        // NOTE: product.stockQuantity is intentionally untouched to protect inventory integrity

        Product updated = productRepository.save(product);
        log.info("Product updated successfully with id: {}, SKU: {}", updated.getId(), updated.getSku());
        return ProductResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (stockTransactionRepository.existsByProductId(id)) {
            throw new ConflictException("Cannot delete product because it has associated transaction records. Deactivate the product instead.");
        }

        if (product.getStockQuantity() > 0) {
            throw new ConflictException("Cannot delete product with non-zero stock quantity (" + product.getStockQuantity() + "). Clear stock or deactivate the product instead.");
        }

        productRepository.delete(product);
        log.info("Product deleted successfully with id: {}", id);
    }

    @Override
    @Transactional
    public ProductResponse updateProductStatus(Long id, com.api.v1.inventory.model.request.UpdateProductStatusRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setActive(request.getActive());
        // NOTE: stockQuantity and all other product fields are preserved untouched
        Product updated = productRepository.save(product);
        log.info("Product status updated successfully: id={}, active={}", updated.getId(), updated.isActive());
        return ProductResponse.fromEntity(updated);
    }
}
