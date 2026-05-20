package com.veltro.inventory.repository;

import com.veltro.inventory.model.AlertEntity;
import com.veltro.inventory.model.AlertSeverity;
import com.veltro.inventory.model.AlertType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<AlertEntity, Long> {

    Optional<AlertEntity> findByIdAndActiveTrueAndBusinessId(Long id, Long businessId);

    List<AlertEntity> findByProductIdAndResolvedFalseAndBusinessId(Long productId, Long businessId);

    boolean existsByProductIdAndResolvedFalseAndTypeAndBusinessId(Long productId, AlertType type, Long businessId);

    @EntityGraph(attributePaths = {"product"})
    @Query("""
        SELECT a FROM AlertEntity a
        WHERE a.resolved = false AND a.businessId = :businessId
        ORDER BY CASE a.severity
            WHEN com.veltro.inventory.model.AlertSeverity.CRITICAL THEN 3
            WHEN com.veltro.inventory.model.AlertSeverity.WARNING THEN 2
            WHEN com.veltro.inventory.model.AlertSeverity.INFO THEN 1
            ELSE 0
        END DESC, a.createdAt ASC
        """)
    Page<AlertEntity> findByResolvedFalseAndBusinessIdOrderBySeverityDescCreatedAtAsc(@Param("businessId") Long businessId, Pageable pageable);

    @EntityGraph(attributePaths = {"product"})
    Page<AlertEntity> findBySeverityAndResolvedFalseAndBusinessIdOrderByCreatedAtDesc(AlertSeverity severity, Long businessId, Pageable pageable);

    long countByReadFalseAndResolvedFalseAndBusinessId(Long businessId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE AlertEntity alert
           SET alert.read = true
         WHERE alert.businessId = :businessId
           AND alert.read = false
           AND alert.resolved = false
        """)
    int markAllAsReadByBusinessId(@Param("businessId") Long businessId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE AlertEntity alert
           SET alert.resolved = true
         WHERE alert.businessId = :businessId
           AND alert.resolved = false
        """)
    int resolveAllByBusinessId(@Param("businessId") Long businessId);
}
