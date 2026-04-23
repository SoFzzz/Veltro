package com.veltro.inventory.dto.catalog;

import java.util.List;

/**
 * Response record for a category, including its immediate subcategories.
 *
 * The recursive {@code subCategories} list enables the Composite Pattern
 * representation in API responses 窶・a single root category can carry the
 * entire tree depth in one response.
 */
public record CategoryResponse(
        Long id,
        String name,
        String description,
        Long parentCategoryId,
        boolean active,
        List<CategoryResponse> subCategories
) {
}

