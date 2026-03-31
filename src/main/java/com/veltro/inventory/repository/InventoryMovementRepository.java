package com.veltro.inventory.repository;

import com.veltro.inventory.model.InventoryMovementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovementEntity, Long> {

    Page<InventoryMovementEntity> findByInventoryIdAndBusinessId(Long inventoryId, Long businessId, Pageable pageable);
}
