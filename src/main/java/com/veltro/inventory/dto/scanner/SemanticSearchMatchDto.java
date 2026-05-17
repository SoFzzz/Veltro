package com.veltro.inventory.dto.scanner;

import java.math.BigDecimal;

public record SemanticSearchMatchDto(
        Long id,
        String name,
        BigDecimal salePrice,
        String barcode,
        String sku
) {
}
