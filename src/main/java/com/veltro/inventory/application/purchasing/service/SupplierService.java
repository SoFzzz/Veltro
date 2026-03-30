package com.veltro.inventory.application.purchasing.service;

import com.veltro.inventory.dto.CreateSupplierRequest;
import com.veltro.inventory.dto.SupplierResponse;
import com.veltro.inventory.dto.UpdateSupplierRequest;
import com.veltro.inventory.mapper.SupplierMapper;
import com.veltro.inventory.model.SupplierEntity;
import com.veltro.inventory.domain.purchasing.ports.SupplierRepository;
import com.veltro.inventory.exception.DuplicateResourceException;
import com.veltro.inventory.exception.NotFoundException;
import com.veltro.inventory.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Application service for supplier management (B2-04).
 *
 * <p>Manages supplier CRUD operations with tax ID uniqueness validation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Finds all active suppliers.
     *
     * @return list of supplier responses
     */
    @Transactional(readOnly = true)
    public List<SupplierResponse> findAll() {
        Long businessId = TenantContext.getBusinessId();
        return supplierRepository.findAllByActiveTrueAndBusinessId(businessId).stream()
                .map(supplierMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Finds a supplier by ID.
     *
     * @param id supplier ID
     * @return supplier response
     * @throws NotFoundException if supplier not found or inactive
     */
    @Transactional(readOnly = true)
    public SupplierResponse findById(Long id) {
        Long businessId = TenantContext.getBusinessId();
        SupplierEntity supplier = supplierRepository.findByIdAndActiveTrueAndBusinessId(id, businessId)
                .orElseThrow(() -> new NotFoundException("Supplier not found with id: " + id));
        
        log.info("Retrieved supplier: {}", supplier.getTaxId());
        return supplierMapper.toResponse(supplier);
    }

    /**
     * Finds a supplier by tax ID.
     *
     * @param taxId tax ID
     * @return supplier response
     * @throws NotFoundException if supplier not found or inactive
     */
    @Transactional(readOnly = true)
    public SupplierResponse findByTaxId(String taxId) {
        Long businessId = TenantContext.getBusinessId();
        SupplierEntity supplier = supplierRepository.findByTaxIdAndActiveTrueAndBusinessId(taxId, businessId)
                .orElseThrow(() -> new NotFoundException("Supplier not found with tax ID: " + taxId));
        
        return supplierMapper.toResponse(supplier);
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    /**
     * Creates a new supplier with tax ID uniqueness validation.
     *
     * @param request create supplier request
     * @return created supplier response
     * @throws DuplicateResourceException if tax ID already exists
     */
    @Transactional
    public SupplierResponse create(CreateSupplierRequest request) {
        Long businessId = TenantContext.getBusinessId();

        // Validate tax ID uniqueness
        if (supplierRepository.existsByTaxIdAndActiveTrueAndIdNotAndBusinessId(request.taxId(), null, businessId)) {
            throw new DuplicateResourceException("Supplier with tax ID '" + request.taxId() + "' already exists");
        }

        SupplierEntity supplier = supplierMapper.toEntity(request);
        supplier.setBusinessId(businessId);
        SupplierEntity saved = supplierRepository.save(supplier);

        log.info("Created supplier: {} with tax ID: {}", saved.getCompanyName(), saved.getTaxId());
        return supplierMapper.toResponse(saved);
    }

    /**
     * Updates an existing supplier. Tax ID cannot be changed.
     *
     * @param id supplier ID
     * @param request update supplier request
     * @return updated supplier response
     * @throws NotFoundException if supplier not found
     */
    @Transactional
    public SupplierResponse update(Long id, UpdateSupplierRequest request) {
        Long businessId = TenantContext.getBusinessId();
        SupplierEntity supplier = supplierRepository.findByIdAndActiveTrueAndBusinessId(id, businessId)
                .orElseThrow(() -> new NotFoundException("Supplier not found with id: " + id));

        supplierMapper.updateEntity(request, supplier);
        SupplierEntity updated = supplierRepository.save(supplier);

        log.info("Updated supplier: {} (tax ID: {})", updated.getCompanyName(), updated.getTaxId());
        return supplierMapper.toResponse(updated);
    }

    /**
     * Soft deletes a supplier by setting active = false.
     *
     * @param id supplier ID
     * @throws NotFoundException if supplier not found
     */
    @Transactional
    public void delete(Long id) {
        Long businessId = TenantContext.getBusinessId();
        SupplierEntity supplier = supplierRepository.findByIdAndActiveTrueAndBusinessId(id, businessId)
                .orElseThrow(() -> new NotFoundException("Supplier not found with id: " + id));

        supplier.setActive(false);
        supplierRepository.save(supplier);

        log.info("Soft deleted supplier: {} (tax ID: {})", supplier.getCompanyName(), supplier.getTaxId());
    }
}
