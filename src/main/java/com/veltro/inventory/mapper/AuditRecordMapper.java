package com.veltro.inventory.mapper;

import com.veltro.inventory.dto.audit.AuditRecordResponse;
import com.veltro.inventory.model.AuditRecordEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapperConfig.class)
public interface AuditRecordMapper {

    /**
     * Maps AuditRecordEntity to AuditRecordResponse DTO.
     * 
     * @param entity the audit record entity
     * @return the response DTO
     */
    AuditRecordResponse toResponse(AuditRecordEntity entity);
}
