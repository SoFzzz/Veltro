package com.veltro.inventory.repository;

import com.veltro.inventory.model.SupplierEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<SupplierEntity, Long> {

    Optional<SupplierEntity> findByIdAndActiveTrueAndBusinessId(Long id, Long businessId);

    Optional<SupplierEntity> findByIdAndActiveFalseAndBusinessId(Long id, Long businessId);

    Optional<SupplierEntity> findByIdAndBusinessId(Long id, Long businessId);

    Page<SupplierEntity> findAllByActiveTrueAndBusinessId(Long businessId, Pageable pageable);

    // Non-paginated variant: suppliers are expected to be short lists in POS.
    List<SupplierEntity> findAllByActiveTrueAndBusinessIdOrderByIdAsc(Long businessId);

    Optional<SupplierEntity> findByTaxIdAndActiveTrueAndBusinessId(String taxId, Long businessId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SupplierEntity s " +
            "WHERE s.taxId = :taxId AND s.active = true AND (:excludeId IS NULL OR s.id != :excludeId) " +
            "AND s.businessId = :businessId")
    boolean existsByTaxIdAndActiveTrueAndIdNotAndBusinessId(
            @Param("taxId") String taxId,
            @Param("excludeId") Long excludeId,
            @Param("businessId") Long businessId);
}
