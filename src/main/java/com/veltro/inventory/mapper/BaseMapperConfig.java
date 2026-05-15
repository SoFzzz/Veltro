package com.veltro.inventory.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

/**
 * Shared configuration for all MapStruct mappers in the project.
 *
 * <p>Centralizes the {@code componentModel = "spring"} configuration to avoid repetition
 * and ensure consistent behavior across all mapping components (Phase 1-1C).
 */
@MapperConfig(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
    uses = SharedMappingUtils.class
)
public interface BaseMapperConfig {
}
