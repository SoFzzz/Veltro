package com.veltro.inventory.mapper;

import com.veltro.inventory.dto.pos.SaleDetailResponse;
import com.veltro.inventory.dto.pos.SaleResponse;
import com.veltro.inventory.model.SaleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for {@link SaleEntity} 竊・{@link SaleResponse} (B2-01).
 *
 * <p>Monetary fields ({@code subtotal}, {@code total}, {@code amountReceived}, {@code change})
 * are converted from {@link BigDecimal} to {@link String} with 4 decimal places (ADR-005).
 *
 * <p>Only active details are included in the response (AC-05).
 */
@Mapper(config = BaseMapperConfig.class)
public abstract class SaleMapper {

    @Autowired
    protected SaleDetailMapper saleDetailMapper;

    @Autowired
    protected SharedMappingUtils sharedMappingUtils;

    @Mapping(target = "subtotal", source = "subtotal", qualifiedByName = "bigDecimalToString")
    @Mapping(target = "total", source = "total", qualifiedByName = "bigDecimalToString")
    @Mapping(target = "amountReceived", source = "amountReceived", qualifiedByName = "bigDecimalToString")
    @Mapping(target = "change", source = "change", qualifiedByName = "bigDecimalToString")
    @Mapping(target = "details", expression = "java(toActiveDetails(entity))")
    @Mapping(target = "auditInfo", expression = "java(sharedMappingUtils.toAuditInfo(entity))")
    public abstract SaleResponse toResponse(SaleEntity entity);

    /**
     * Filters and maps only active details (AC-05).
     */
    protected List<SaleDetailResponse> toActiveDetails(SaleEntity entity) {
        return entity.getDetails().stream()
                .filter(d -> d.isActive())
                .map(saleDetailMapper::toResponse)
                .collect(Collectors.toList());
    }
}

