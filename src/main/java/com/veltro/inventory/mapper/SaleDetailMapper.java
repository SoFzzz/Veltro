package com.veltro.inventory.mapper;

import com.veltro.inventory.dto.pos.SaleDetailResponse;
import com.veltro.inventory.model.SaleDetailEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for {@link SaleDetailEntity} 竊・{@link SaleDetailResponse} (B2-01).
 *
 * <p>Monetary fields ({@code unitPrice}, {@code subtotal}) are converted from {@link BigDecimal}
 * to {@link String} with 4 decimal places (ADR-005).
 */
@Mapper(config = BaseMapperConfig.class)
public interface SaleDetailMapper {

    @Mapping(target = "unitPrice", source = "unitPrice", qualifiedByName = "bigDecimalToString")
    @Mapping(target = "subtotal", source = "subtotal", qualifiedByName = "bigDecimalToString")
    @Mapping(target = "auditInfo", source = ".")
    SaleDetailResponse toResponse(SaleDetailEntity entity);
}

