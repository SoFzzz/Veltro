package com.veltro.inventory.repository;

import com.veltro.inventory.model.InventoryMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA adapter implementing the {@link com.veltro.inventory.domain.inventory.ports.InventoryMovementRepository} output port.
 *
 * {@code findByInventoryId} uses the B-Tree index
 * {@code idx_movements_inventory} created in V2 migration.
 */
@Repository
public interface InventoryMovementRepository
        extends JpaRepository<InventoryMovementEntity, Long>, com.veltro.inventory.domain.inventory.ports.InventoryMovementRepository {
}
