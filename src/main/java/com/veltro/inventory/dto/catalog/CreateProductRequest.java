package com.veltro.inventory.dto.catalog;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request record for creating a new product.
 *
 * Client-side price sanity is validated here with {@code @DecimalMin}.
 * The stricter domain constraint {@code salePrice >= costPrice} is enforced
 * in {@code ProductService} and throws {@code InvalidPriceException}.
 */
public record CreateProductRequest(

        @NotBlank(message = "{validation.product.name.notblank}")
        @Size(max = 200, message = "{validation.product.name.size}")
        String name,

        @Size(max = 100, message = "{validation.product.barcode.size}")
        String barcode,

        @Size(max = 100, message = "{validation.product.sku.size}")
        String sku,

        @Size(max = 500, message = "{validation.product.description.size}")
        String description,

        @NotNull(message = "{validation.product.costprice.required}")
        @DecimalMin(value = "0.0001", message = "{validation.product.costprice.min}")
        BigDecimal costPrice,

        @NotNull(message = "{validation.product.saleprice.required}")
        @DecimalMin(value = "0.0001", message = "{validation.product.saleprice.min}")
        BigDecimal salePrice,

        Long categoryId,

        // Stock alert thresholds (P4 - Inventory Alerts)
        @Min(value = 0, message = "{validation.product.minstockinfo.min}")
        Integer minStockInfo,

        @Min(value = 0, message = "{validation.product.minstockwarning.min}")
        Integer minStockWarning,

        @Min(value = 0, message = "{validation.product.minstockcritical.min}")
        Integer minStockCritical
) {
}

