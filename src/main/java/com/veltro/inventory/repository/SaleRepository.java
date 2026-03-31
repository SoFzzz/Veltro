package com.veltro.inventory.repository;

import com.veltro.inventory.model.SaleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<SaleEntity, Long> {

    Optional<SaleEntity> findByIdAndActiveTrueAndBusinessId(Long id, Long businessId);

    @Query(value = "SELECT nextval('sale_number_seq')", nativeQuery = true)
    Long getNextSaleSequenceValue();
}