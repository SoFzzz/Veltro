package com.veltro.inventory.mapper;

import com.veltro.inventory.dto.purchasing.CreatePurchaseOrderRequest;
import com.veltro.inventory.dto.purchasing.PurchaseOrderDetailResponse;
import com.veltro.inventory.dto.purchasing.PurchaseOrderResponse;
import com.veltro.inventory.dto.audit.AuditInfo;
import com.veltro.inventory.model.PurchaseOrderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for {@link PurchaseOrderEntity} 竊・{@link PurchaseOrderResponse} (B2-04).
 *
 * <p>ADR-005: Monetary fields are converted to String with 4 decimal places.
 * <p>AC-05: Only active details are included in the response.
 */
@Mapper(config = BaseMapperConfig.class)
public abstract class PurchaseOrderMapper {

    @Autowired
    protected PurchaseOrderDetailMapper detailMapper;

    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "supplierName", source = "supplier.companyName")
    @Mapping(target = "total", source = "total", qualifiedByName = "bigDecimalToString")
    @Mapping(target = "details", expression = "java(toActiveDetails(entity))")
    @Mapping(target = "auditInfo", expression = "java(toAuditInfo(entity))")
    public abstract PurchaseOrderResponse toResponse(PurchaseOrderEntity entity);

    /**
     * Maps a create request to a new entity.
     * {@code id}, {@code supplier}, and audit fields are excluded; set by service.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "details", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "requestedBy", ignore = true)
    public abstract PurchaseOrderEntity toEntity(CreatePurchaseOrderRequest request);

    /**
     * Converts BigDecimal to String with 4 decimal places (ADR-005).
     */
    @Named("bigDecimalToString")
    protected String bigDecimalToString(BigDecimal value) {
        return value != null ? value.setScale(4, RoundingMode.HALF_UP).toPlainString() : null;
    }

    /**
     * Filters and maps only active details (AC-05).
     */
    protected List<PurchaseOrderDetailResponse> toActiveDetails(PurchaseOrderEntity entity) {
        return entity.getDetails().stream()
                .filter(d -> d.isActive())
                .map(detailMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Extracts audit information from entity.
     */
    protected AuditInfo toAuditInfo(PurchaseOrderEntity entity) {
        return new AuditInfo(
                instantToLocalDateTime(entity.getCreatedAt()),
                entity.getCreatedBy(),
                instantToLocalDateTime(entity.getUpdatedAt()),
                entity.getUpdatedBy()
        );
    }

    /**
     * Converts Instant to LocalDateTime in system default zone.
     */
    protected LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}
