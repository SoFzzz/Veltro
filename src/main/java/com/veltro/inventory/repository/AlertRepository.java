package com.veltro.inventory.repository;

import com.veltro.inventory.model.AlertEntity;
import com.veltro.inventory.model.AlertType;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertRepository extends JpaRepository<AlertEntity, Long>, com.veltro.inventory.domain.inventory.ports.AlertRepository {

    @Override
    Optional<AlertEntity> findByIdAndActiveTrue(Long id);

    @Override
    List<AlertEntity> findByProductIdAndResolvedFalse(Long productId);

    @Override
    boolean existsByProductIdAndResolvedFalseAndType(Long productId, AlertType type);

    @Override
    Page<AlertEntity> findByResolvedFalseOrderBySeverityDescCreatedAtAsc(Pageable pageable);

    @Override
    Page<AlertEntity> findByReadFalseAndResolvedFalseOrderBySeverityDescCreatedAtAsc(Pageable pageable);

    @Override
    long countByReadFalseAndResolvedFalse();
}
