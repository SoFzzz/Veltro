package com.veltro.inventory.repository;

import com.veltro.inventory.model.InventoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryEntity, Long> {

    Optional<InventoryEntity> findByProductIdAndActiveTrueAndBusinessId(Long productId, Long businessId);

    Page<InventoryEntity> findAllByActiveTrueAndBusinessId(Long businessId, Pageable pageable);
}
