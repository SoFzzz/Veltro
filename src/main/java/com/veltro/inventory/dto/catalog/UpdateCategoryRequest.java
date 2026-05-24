package com.veltro.inventory.dto.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request record for updating an existing category.
 */
public record UpdateCategoryRequest(

        @NotBlank(message = "{validation.category.name.notblank}")
        @Size(max = 30, message = "{validation.category.name.size}")
        String name,

        @Size(max = 40, message = "{validation.category.description.size}")
        String description,

        /** Optional 窶・set to null to move category to root level. */
        Long parentCategoryId
) {
}

