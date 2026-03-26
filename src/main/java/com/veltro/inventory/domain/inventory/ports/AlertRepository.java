package com.veltro.inventory.domain.inventory.ports;

import com.veltro.inventory.domain.inventory.model.AlertEntity;
import com.veltro.inventory.domain.inventory.model.AlertType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AlertRepository {

    AlertEntity save(AlertEntity alert);

    Optional<AlertEntity> findByIdAndActiveTrue(Long id);

    List<AlertEntity> findByProductIdAndResolvedFalse(Long productId);

    boolean existsByProductIdAndResolvedFalseAndType(Long productId, AlertType type);

    Page<AlertEntity> findByResolvedFalseOrderBySeverityDescCreatedAtAsc(Pageable pageable);

    Page<AlertEntity> findByReadFalseAndResolvedFalseOrderBySeverityDescCreatedAtAsc(Pageable pageable);

    long countByReadFalseAndResolvedFalse();

    // --- Multi-tenant scoped methods ---

    Optional<AlertEntity> findByIdAndActiveTrueAndBusinessId(Long id, Long businessId);

    List<AlertEntity> findByProductIdAndResolvedFalseAndBusinessId(Long productId, Long businessId);

    boolean existsByProductIdAndResolvedFalseAndTypeAndBusinessId(Long productId, AlertType type, Long businessId);

    Page<AlertEntity> findByResolvedFalseAndBusinessIdOrderBySeverityDescCreatedAtAsc(Long businessId, Pageable pageable);

    Page<AlertEntity> findByReadFalseAndResolvedFalseAndBusinessIdOrderBySeverityDescCreatedAtAsc(Long businessId, Pageable pageable);

    long countByReadFalseAndResolvedFalseAndBusinessId(Long businessId);
}
