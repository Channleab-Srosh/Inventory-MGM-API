package com.api.v1.inventory.service;

import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.request.CreateProductRequest;
import com.api.v1.inventory.model.request.UpdateProductRequest;
import com.api.v1.inventory.model.response.ProductResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * Uploads an image file for the given product, stores it locally, and
     * updates the product's {@code imageUrl}. Any previously stored image
     * managed by this application is deleted.
     *
     * @param id   the product ID
     * @param file the image file (JPEG / PNG / GIF / WEBP, max 5 MB)
     * @return updated product response containing the new {@code imageUrl}
     */
    ProductResponse uploadProductImage(Long id, MultipartFile file);

    void deleteProduct(Long id);

    void deleteProducts(java.util.List<Long> ids);
}

