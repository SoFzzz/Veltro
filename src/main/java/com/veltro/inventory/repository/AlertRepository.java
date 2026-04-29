package com.veltro.inventory.repository;

import com.veltro.inventory.model.AlertEntity;
import com.veltro.inventory.model.AlertSeverity;
import com.veltro.inventory.model.AlertType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<AlertEntity, Long> {

    Optional<AlertEntity> findByIdAndActiveTrueAndBusinessId(Long id, Long businessId);

    List<AlertEntity> findByProductIdAndResolvedFalseAndBusinessId(Long productId, Long businessId);

    boolean existsByProductIdAndResolvedFalseAndType(Long productId, AlertType type);

    Page<AlertEntity> findByResolvedFalseAndBusinessIdOrderBySeverityDescCreatedAtAsc(Long businessId, Pageable pageable);

    Page<AlertEntity> findBySeverityAndResolvedFalseAndBusinessIdOrderByCreatedAtDesc(AlertSeverity severity, Long businessId, Pageable pageable);

    Page<AlertEntity> findByReadFalseAndResolvedFalseOrderBySeverityDescCreatedAtAsc(Pageable pageable);

    long countByReadFalseAndResolvedFalseAndBusinessId(Long businessId);
}