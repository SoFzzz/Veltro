package com.veltro.inventory.mapper;

import com.veltro.inventory.dto.inventory.InventoryMovementResponse;
import com.veltro.inventory.model.InventoryMovementEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for {@link InventoryMovementEntity} 竊・movement DTOs.
 *
 * {@code movementType} is the string representation of the enum
 * (MapStruct maps enum 竊・String by default via {@code name()}).
 */
@Mapper(config = BaseMapperConfig.class)
public interface InventoryMovementMapper {

    @Mapping(target = "inventoryId", source = "inventory.id")
    @Mapping(target = "movementType", source = "movementType")
    InventoryMovementResponse toResponse(InventoryMovementEntity entity);
}

