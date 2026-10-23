package com.api.v1.inventory.service.impl;

import com.api.v1.inventory.common.exception.BadRequestException;
import com.api.v1.inventory.common.exception.ConflictException;
import com.api.v1.inventory.common.exception.ResourceNotFoundException;
import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.entity.Category;
import com.api.v1.inventory.model.request.CreateCategoryRequest;
import com.api.v1.inventory.model.request.UpdateCategoryRequest;
import com.api.v1.inventory.model.response.CategoryResponse;
import com.api.v1.inventory.repository.CategoryRepository;
import com.api.v1.inventory.repository.ProductRepository;
import com.api.v1.inventory.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String trimmedName = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new ConflictException("Category with name '" + trimmedName + "' already exists");
        }

        Category category = Category.builder()
                .name(trimmedName)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Category created successfully with id: {}", saved.getId());
        return CategoryResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getAllCategories(String search, Pageable pageable) {
        Page<Category> categoryPage;
        if (search != null && !search.trim().isEmpty()) {
            categoryPage = categoryRepository.findByNameContainingIgnoreCase(search.trim(), pageable);
        } else {
            categoryPage = categoryRepository.findAll(pageable);
        }
        return PageResponse.from(categoryPage.map(CategoryResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return CategoryResponse.fromEntity(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        String trimmedName = request.getName().trim();
        if (!category.getName().equalsIgnoreCase(trimmedName) &&
                categoryRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new ConflictException("Category with name '" + trimmedName + "' already exists");
        }

        category.setName(trimmedName);
        category.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);

        Category updated = categoryRepository.save(category);
        log.info("Category updated successfully with id: {}", updated.getId());
        return CategoryResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        deleteCategories(List.of(id));
    }

    @Override
    @Transactional
    public void deleteCategories(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("No category IDs provided for deletion");
        }

        // 1. Single query to load all requested categories
        List<Category> categories = categoryRepository.findAllById(ids);
        Set<Long> foundIds = categories.stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        // 2. Identify missing IDs and reject with 404 listing ALL missing IDs
        List<Long> missingIds = ids.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        if (!missingIds.isEmpty()) {
            String msg = (missingIds.size() == 1)
                    ? "Category not found with id: " + missingIds.get(0) + ". Nothing was deleted."
                    : "Categories not found with IDs: " + missingIds + ". Nothing was deleted.";
            throw new ResourceNotFoundException(msg);
        }

        // 3. Check for product references (409 Conflict)
        List<Long> referencedIds = productRepository.findReferencedCategoryIds(ids);
        if (!referencedIds.isEmpty()) {
            throw new ConflictException(
                    "Cannot delete category because it is currently referenced by one or more products. Referenced category IDs: "
                            + referencedIds + ". Nothing was deleted."
            );
        }

        // 4. Atomic delete
        categoryRepository.deleteAll(categories);
        log.info("Deleted {} category(ies) with ids: {}", categories.size(), ids);
    }
}
