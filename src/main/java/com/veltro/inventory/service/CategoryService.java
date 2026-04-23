package com.veltro.inventory.service;

import com.veltro.inventory.dto.catalog.CategoryResponse;
import com.veltro.inventory.dto.catalog.CreateCategoryRequest;
import com.veltro.inventory.dto.catalog.UpdateCategoryRequest;
import com.veltro.inventory.mapper.CategoryMapper;
import com.veltro.inventory.model.CategoryEntity;
import com.veltro.inventory.repository.CategoryRepository;
import com.veltro.inventory.exception.DuplicateResourceException;
import com.veltro.inventory.exception.InactiveResourceExistsException;
import com.veltro.inventory.exception.NotFoundException;
import com.veltro.inventory.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

        // BUG-07: Check for existing category with same name (active or inactive)
        checkForDuplicateName(request.name(), businessId, null);

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
        checkForDuplicateName(request.name(), businessId, id);
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

    /**
     * Reactivates a soft-deleted category (BUG-14 fix).
     * Sets {@code active=true} so the category appears in listings again.
     */
    @Transactional
    public CategoryResponse reactivate(Long id) {
        Long businessId = TenantContext.getBusinessId();
        CategoryEntity entity = categoryRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));

        if (entity.isActive()) {
            throw new IllegalArgumentException("Category with id " + id + " is already active.");
        }

        entity.setActive(true);
        CategoryEntity saved = categoryRepository.save(entity);
        log.info("Category reactivated: id={}", id);
        return categoryMapper.toResponse(saved);
    }

    /**
     * Hard-deletes a category. Allowed only if the category belongs to the current tenant.
     */
    @Transactional
    public void hardDelete(Long id) {
        Long businessId = TenantContext.getBusinessId();
        if (!categoryRepository.existsByIdAndBusinessId(id, businessId)) {
            throw new NotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
        log.info("Category hard deleted: id={}", id);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Checks if a category with the given name already exists for the business.
     * Distinguishes between active duplicates (error) and inactive ones (suggest reactivation).
     * BUG-07 fix.
     */
    private void checkForDuplicateName(String name, Long businessId, Long currentCategoryId) {
        Optional<CategoryEntity> existing = categoryRepository.findByNameAndBusinessId(name, businessId);
        
        if (existing.isPresent()) {
            CategoryEntity category = existing.get();
            if (currentCategoryId != null && currentCategoryId.equals(category.getId())) {
                return;
            }
            if (category.isActive()) {
                throw new DuplicateResourceException("Category", "name", name);
            } else {
                throw new InactiveResourceExistsException("category", "name", name, category.getId());
            }
        }
    }

    private CategoryEntity requireActive(Long id, Long businessId) {
        return categoryRepository.findByIdAndActiveTrueAndBusinessId(id, businessId)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));
    }
}

