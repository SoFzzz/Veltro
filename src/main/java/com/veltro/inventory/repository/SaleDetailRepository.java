package com.veltro.inventory.repository;

import com.veltro.inventory.model.SaleDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SaleDetailRepository extends JpaRepository<SaleDetailEntity, Long> {
}