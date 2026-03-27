package com.veltro.inventory.infrastructure.adapters.persistence;

import com.veltro.inventory.model.AuditAction;
import com.veltro.inventory.model.AuditEntityType;
import com.veltro.inventory.model.AuditRecordEntity;
import com.veltro.inventory.domain.audit.ports.AuditRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * JPA repository adapter for audit records (B3-03).
 * 
 * <p>Implements the {@link AuditRecordRepository} port defined in the domain layer.
 * Provides custom queries for filtering by entity type, action, username, and date range.
 */
@Repository
public interface AuditRecordJpaRepository extends JpaRepository<AuditRecordEntity, Long>, AuditRecordRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    List<AuditRecordEntity> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            AuditEntityType entityType, Long entityId);

    /**
     * {@inheritDoc}
     * 
     * <p>Custom JPQL query with dynamic filters. Null parameters are ignored.
     * Results ordered by created_at DESC for recent-first display.
     */
    @Override
    @Query("""
            SELECT a FROM AuditRecordEntity a
            WHERE (CAST(:entityType AS string) IS NULL OR a.entityType = :entityType)
              AND (CAST(:action AS string) IS NULL OR a.action = :action)
              AND (:username IS NULL OR a.username = :username)
              AND (CAST(:startDate AS string) IS NULL OR a.createdAt >= :startDate)
              AND (CAST(:endDate AS string) IS NULL OR a.createdAt <= :endDate)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditRecordEntity> findByFilters(
            @Param("entityType") AuditEntityType entityType,
            @Param("action") AuditAction action,
            @Param("username") String username,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    // --- Multi-tenant scoped methods ---

    @Override
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

    @Override
    List<AuditRecordEntity> findByEntityTypeAndEntityIdAndBusinessIdOrderByCreatedAtDesc(
            AuditEntityType entityType, Long entityId, Long businessId);
}
