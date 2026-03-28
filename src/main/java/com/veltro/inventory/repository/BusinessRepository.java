package com.veltro.inventory.repository;

import com.veltro.inventory.model.BusinessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusinessRepository extends JpaRepository<BusinessEntity, Long> {
    Optional<BusinessEntity> findByName(String name);
    boolean existsByName(String name);
}