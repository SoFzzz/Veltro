package com.veltro.inventory.repository;

import com.veltro.inventory.model.AuditAction;
import com.veltro.inventory.model.AuditEntityType;
import com.veltro.inventory.model.AuditRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditRecordRepository extends JpaRepository<AuditRecordEntity, Long> {

    @Query("""
            SELECT a FROM AuditRecordEntity a
            WHERE (CAST(:entityType AS string) IS NULL OR a.entityType = :entityType)
              AND (CAST(:action AS string) IS NULL OR a.action = :action)
              AND (:username IS NULL OR a.username = :username)
              AND (CAST(:startDate AS string) IS NULL OR a.createdAt >= :startDate)
              AND (CAST(:endDate AS string) IS NULL OR a.createdAt <= :endDate)
              AND a.businessId = :businessId
            ORDER BY a.createdAt DESC
            """)
    Page<AuditRecordEntity> findByFiltersAndBusinessId(
            @Param("entityType") AuditEntityType entityType,
            @Param("action") AuditAction action,
            @Param("username") String username,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("businessId") Long businessId,
            Pageable pageable);

    List<AuditRecordEntity> findByEntityTypeAndEntityIdAndBusinessIdOrderByCreatedAtDesc(
            AuditEntityType entityType, Long entityId, Long businessId);
}
