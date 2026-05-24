package com.veltro.inventory.dto.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request record for creating a new category.
 */
public record CreateCategoryRequest(

        @NotBlank(message = "{validation.category.name.notblank}")
        @Size(max = 30, message = "{validation.category.name.size}")
        String name,

        @Size(max = 40, message = "{validation.category.description.size}")
        String description,

        /** Optional 窶・null means this is a root category. */
        Long parentCategoryId
) {
}

