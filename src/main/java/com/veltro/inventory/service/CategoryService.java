package com.veltro.inventory.service;

import com.veltro.inventory.dto.CategoryResponse;
import com.veltro.inventory.dto.CreateCategoryRequest;
import com.veltro.inventory.dto.UpdateCategoryRequest;
import com.veltro.inventory.mapper.CategoryMapper;
import com.veltro.inventory.model.CategoryEntity;
import com.veltro.inventory.domain.catalog.ports.CategoryRepository;
import com.veltro.inventory.exception.NotFoundException;
import com.veltro.inventory.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for category management (B1-03).
 *
 * Owns all {@link Transactional} boundaries for category operations.
 * Delegates persistence to the {@link CategoryRepository} domain port.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Returns all root categories (those with no parent) with their subcategory
     * trees populated. This is the primary tree-view endpoint.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> findRoots() {
        Long businessId = TenantContext.getBusinessId();
        return categoryRepository.findAllByParentCategoryIsNullAndActiveTrueAndBusinessId(businessId)
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        Long businessId = TenantContext.getBusinessId();
        CategoryEntity entity = requireActive(id, businessId);
        return categoryMapper.toResponse(entity);
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        Long businessId = TenantContext.getBusinessId();
        CategoryEntity entity = categoryMapper.toEntity(request);
        entity.setBusinessId(businessId);

        if (request.parentCategoryId() != null) {
            CategoryEntity parent = requireActive(request.parentCategoryId(), businessId);
            entity.setParentCategory(parent);
        }

        CategoryEntity saved = categoryRepository.save(entity);
        log.info("Category created: id={}, name={}", saved.getId(), saved.getName());
        return categoryMapper.toResponse(saved);
    }

    @Transactional
    public CategoryResponse update(Long id, UpdateCategoryRequest request) {
        Long businessId = TenantContext.getBusinessId();
        CategoryEntity entity = requireActive(id, businessId);
        categoryMapper.updateEntity(request, entity);

        if (request.parentCategoryId() != null) {
            CategoryEntity parent = requireActive(request.parentCategoryId(), businessId);
            entity.setParentCategory(parent);
        } else {
            entity.setParentCategory(null);
        }

        CategoryEntity saved = categoryRepository.save(entity);
        log.info("Category updated: id={}, name={}", saved.getId(), saved.getName());
        return categoryMapper.toResponse(saved);
    }

    /**
     * Soft-deletes a category (AC-05). The record remains in the database
     * with {@code active=false} and will not appear in active listings.
     */
    @Transactional
    public void deactivate(Long id) {
        Long businessId = TenantContext.getBusinessId();
        CategoryEntity entity = requireActive(id, businessId);
        entity.setActive(false);
        categoryRepository.save(entity);
        log.info("Category deactivated: id={}", id);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private CategoryEntity requireActive(Long id, Long businessId) {
        return categoryRepository.findByIdAndActiveTrueAndBusinessId(id, businessId)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));
    }
}
