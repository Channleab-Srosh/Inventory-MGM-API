package com.api.v1.inventory.service;

import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.request.CreateProductRequest;
import com.api.v1.inventory.model.request.UpdateProductRequest;
import com.api.v1.inventory.model.response.ProductResponse;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);

    PageResponse<ProductResponse> getProducts(
            String keyword,
            Long categoryId,
            Long supplierId,
            Boolean active,
            Pageable pageable
    );

    ProductResponse getProductById(Long id);

    ProductResponse getProductBySku(String sku);

    ProductResponse updateProduct(Long id, UpdateProductRequest request);

    ProductResponse updateProductStatus(Long id, com.api.v1.inventory.model.request.UpdateProductStatusRequest request);

    void deleteProduct(Long id);

    void deleteProducts(java.util.List<Long> ids);
}
