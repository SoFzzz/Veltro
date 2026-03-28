package com.veltro.inventory.repository;

import com.veltro.inventory.model.PurchaseOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrderEntity, Long> {

    List<PurchaseOrderEntity> findAllByActiveTrueAndBusinessId(Long businessId);

    List<PurchaseOrderEntity> findBySupplierIdAndActiveTrueAndBusinessId(Long supplierId, Long businessId);

    Optional<PurchaseOrderEntity> findByIdAndActiveTrueAndBusinessId(Long id, Long businessId);

    Optional<PurchaseOrderEntity> findByOrderNumberAndActiveTrueAndBusinessId(String orderNumber, Long businessId);

    @Query(value = "SELECT nextval('purchase_order_number_seq')", nativeQuery = true)
    Long getNextOrderSequenceValue();
}