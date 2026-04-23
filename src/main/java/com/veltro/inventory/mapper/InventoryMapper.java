package com.veltro.inventory.mapper;

import com.veltro.inventory.dto.inventory.InventoryResponse;
import com.veltro.inventory.model.InventoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for {@link InventoryEntity} 竊・inventory DTOs.
 */
@Mapper(componentModel = "spring")
public interface InventoryMapper {

    /**
     * Maps entity to response DTO.
     * Product name and id are sourced from the lazy association.
     */
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    InventoryResponse toResponse(InventoryEntity entity);
}

