package com.veltro.inventory.repository;

import com.veltro.inventory.model.PurchaseOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for {@link PurchaseOrderEntity} (B2-04).
 *
 * <p>Extends the domain {@link com.veltro.inventory.domain.purchasing.ports.PurchaseOrderRepository} port and provides
 * a native query to fetch the next value from the PostgreSQL sequence.
 */
@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrderEntity, Long>, com.veltro.inventory.domain.purchasing.ports.PurchaseOrderRepository {

    /**
     * Fetches the next value from the purchase_order_number_seq PostgreSQL sequence.
     *
     * @return the next sequence value
     */
    @Query(value = "SELECT nextval('purchase_order_number_seq')", nativeQuery = true)
    Long getNextOrderSequenceValue();
}