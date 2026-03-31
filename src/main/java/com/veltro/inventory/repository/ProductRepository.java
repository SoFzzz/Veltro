package com.veltro.inventory.repository;

import com.veltro.inventory.model.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    Page<ProductEntity> findAllByActiveTrueAndBusinessId(Long businessId, Pageable pageable);

    Optional<ProductEntity> findByBarcodeAndActiveTrueAndBusinessId(String barcode, Long businessId);

    Optional<ProductEntity> findByIdAndActiveTrueAndBusinessId(Long id, Long businessId);
}
