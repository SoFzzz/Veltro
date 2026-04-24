package com.veltro.inventory.service;

import com.veltro.inventory.dto.purchasing.CreateSupplierRequest;
import com.veltro.inventory.dto.purchasing.SupplierResponse;
import com.veltro.inventory.dto.purchasing.UpdateSupplierRequest;
import com.veltro.inventory.mapper.SupplierMapper;
import com.veltro.inventory.model.SupplierEntity;
import com.veltro.inventory.repository.SupplierRepository;
import com.veltro.inventory.exception.DuplicateResourceException;
import com.veltro.inventory.exception.NotFoundException;
import com.veltro.inventory.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        return supplierRepository.findAllByActiveTrueAndBusinessIdOrderByIdAsc(businessId)
                .stream()
                .map(supplierMapper::toResponse)
                .toList();
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

    /**
     * Activates a previously deactivated supplier.
     *
     * @param id supplier ID
     * @return activated supplier response
     * @throws NotFoundException if supplier not found or already active
     */
    @Transactional
    public SupplierResponse activate(Long id) {
        Long businessId = TenantContext.getBusinessId();
        SupplierEntity supplier = supplierRepository.findByIdAndActiveFalseAndBusinessId(id, businessId)
                .orElseThrow(() -> new NotFoundException("Inactive supplier not found with id: " + id));

        // Check for tax ID conflict with an active supplier
        if (supplierRepository.existsByTaxIdAndActiveTrueAndIdNotAndBusinessId(supplier.getTaxId(), id, businessId)) {
            throw new DuplicateResourceException("Cannot activate: another active supplier already has tax ID '" + supplier.getTaxId() + "'");
        }

        supplier.setActive(true);
        SupplierEntity activated = supplierRepository.save(supplier);

        log.info("Activated supplier: {} (tax ID: {})", activated.getCompanyName(), activated.getTaxId());
        return supplierMapper.toResponse(activated);
    }

    /**
     * Deactivates a supplier (soft delete with explicit semantics).
     *
     * @param id supplier ID
     * @return deactivated supplier response
     * @throws NotFoundException if supplier not found or already inactive
     */
    @Transactional
    public SupplierResponse deactivate(Long id) {
        Long businessId = TenantContext.getBusinessId();
        SupplierEntity supplier = supplierRepository.findByIdAndActiveTrueAndBusinessId(id, businessId)
                .orElseThrow(() -> new NotFoundException("Active supplier not found with id: " + id));

        supplier.setActive(false);
        SupplierEntity deactivated = supplierRepository.save(supplier);

        log.info("Deactivated supplier: {} (tax ID: {})", deactivated.getCompanyName(), deactivated.getTaxId());
        return supplierMapper.toResponse(deactivated);
    }
}

