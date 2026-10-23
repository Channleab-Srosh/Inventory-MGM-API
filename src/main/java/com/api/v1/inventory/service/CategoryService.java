package com.api.v1.inventory.service;

import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.request.CreateCategoryRequest;
import com.api.v1.inventory.model.request.UpdateCategoryRequest;
import com.api.v1.inventory.model.response.CategoryResponse;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    CategoryResponse createCategory(CreateCategoryRequest request);

    PageResponse<CategoryResponse> getAllCategories(String search, Pageable pageable);

    CategoryResponse getCategoryById(Long id);

    CategoryResponse updateCategory(Long id, UpdateCategoryRequest request);

    void deleteCategory(Long id);

    void deleteCategories(java.util.List<Long> ids);
}
