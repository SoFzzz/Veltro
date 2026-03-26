package com.veltro.inventory.domain.iam.ports;

import com.veltro.inventory.domain.iam.model.BusinessEntity;

import java.util.Optional;

/**
 * Domain port (output) for business persistence (multi-tenant).
 *
 * Each business represents an isolated data namespace. An ADMIN user owns
 * one business; CASHIER/WAREHOUSE workers are linked to their admin's business.
 */
public interface BusinessRepository {

    BusinessEntity save(BusinessEntity business);

    Optional<BusinessEntity> findById(Long id);

    Optional<BusinessEntity> findByIdAndActiveTrue(Long id);
}
