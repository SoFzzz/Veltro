package com.veltro.inventory.repository;

import com.veltro.inventory.model.PurchaseOrderEntity;
import com.veltro.inventory.model.PurchaseOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrderEntity, Long> {

    Page<PurchaseOrderEntity> findAllByActiveTrueAndBusinessId(Long businessId, Pageable pageable);

    Page<PurchaseOrderEntity> findAllByActiveTrueAndStatusAndBusinessId(PurchaseOrderStatus status, Long businessId, Pageable pageable);

    Page<PurchaseOrderEntity> findBySupplierIdAndActiveTrueAndBusinessId(Long supplierId, Long businessId, Pageable pageable);

    // Non-paginated variants: POS purchase orders are short lists.
    List<PurchaseOrderEntity> findAllByActiveTrueAndBusinessIdOrderByIdAsc(Long businessId);

    List<PurchaseOrderEntity> findAllByActiveTrueAndStatusAndBusinessIdOrderByIdAsc(PurchaseOrderStatus status, Long businessId);

    List<PurchaseOrderEntity> findBySupplierIdAndActiveTrueAndBusinessIdOrderByIdAsc(Long supplierId, Long businessId);

    Optional<PurchaseOrderEntity> findByIdAndActiveTrueAndBusinessId(Long id, Long businessId);

    Optional<PurchaseOrderEntity> findByOrderNumberAndActiveTrueAndBusinessId(String orderNumber, Long businessId);

    @Query(value = "SELECT nextval('purchase_order_number_seq')", nativeQuery = true)
    Long getNextOrderSequenceValue();
}
