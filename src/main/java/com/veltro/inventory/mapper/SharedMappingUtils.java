package com.veltro.inventory.mapper;

import com.veltro.inventory.dto.audit.AuditInfo;
import com.veltro.inventory.model.AbstractAuditableEntity;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class SharedMappingUtils {

    @Named("bigDecimalToString")
    public String bigDecimalToString(BigDecimal value) {
        return value != null
                ? value.setScale(4, RoundingMode.HALF_UP).toPlainString()
                : null;
    }

    public LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant != null
                ? LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
                : null;
    }

    public AuditInfo toAuditInfo(AbstractAuditableEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AuditInfo(
                instantToLocalDateTime(entity.getCreatedAt()),
                entity.getCreatedBy(),
                instantToLocalDateTime(entity.getUpdatedAt()),
                entity.getUpdatedBy()
        );
    }
}
