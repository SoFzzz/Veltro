package com.veltro.inventory.repository;

import com.veltro.inventory.model.AlertConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlertConfigurationRepository extends JpaRepository<AlertConfigurationEntity, Long> {

    Optional<AlertConfigurationEntity> findByProductIdAndActiveTrueAndBusinessId(Long productId, Long businessId);
}
