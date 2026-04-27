package com.veltro.inventory.mapper;

import com.veltro.inventory.dto.inventory.AlertConfigurationResponse;
import com.veltro.inventory.model.AlertConfigurationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = BaseMapperConfig.class)
public interface AlertConfigurationMapper {

    @Mapping(target = "productId", expression = "java(entity.getProduct() != null ? entity.getProduct().getId() : null)")
    @Mapping(target = "criticalStock", source = "criticalStock")
    @Mapping(target = "minStock", source = "minStock")
    @Mapping(target = "overstockThreshold", source = "overstockThreshold")
    AlertConfigurationResponse toResponse(AlertConfigurationEntity entity);
}

