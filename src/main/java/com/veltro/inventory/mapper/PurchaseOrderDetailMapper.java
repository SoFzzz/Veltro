package com.veltro.inventory.mapper;

import com.veltro.inventory.dto.purchasing.PurchaseOrderDetailResponse;
import com.veltro.inventory.model.PurchaseOrderDetailEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

/**
 * MapStruct mapper for {@link PurchaseOrderDetailEntity} 竊・{@link PurchaseOrderDetailResponse} (B2-04).
 *
 * <p>ADR-005: Monetary fields are converted to String with 4 decimal places.
 */
@Mapper(config = BaseMapperConfig.class)
public abstract class PurchaseOrderDetailMapper {

    @Autowired
    protected SharedMappingUtils sharedMappingUtils;

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "unitCost", source = "unitCost", qualifiedByName = "bigDecimalToString")
    @Mapping(target = "subtotal", expression = "java(calculateSubtotal(entity))")
    @Mapping(target = "auditInfo", expression = "java(sharedMappingUtils.toAuditInfo(entity))")
    public abstract PurchaseOrderDetailResponse toResponse(PurchaseOrderDetailEntity entity);

    /**
     * Calculates subtotal as unitCost * requestedQuantity.
     */
    protected String calculateSubtotal(PurchaseOrderDetailEntity entity) {
        if (entity.getUnitCost() == null || entity.getRequestedQuantity() == null) {
            return "0.0000";
        }
        BigDecimal subtotal = entity.getUnitCost().multiply(BigDecimal.valueOf(entity.getRequestedQuantity()));
        return sharedMappingUtils.bigDecimalToString(subtotal);
    }
}
