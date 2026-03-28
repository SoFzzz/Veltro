package com.veltro.inventory.application.purchasing.service;

import static org.mockito.ArgumentMatchers.anyLong;
import com.veltro.inventory.dto.CreateSupplierRequest;
import com.veltro.inventory.dto.SupplierResponse;
import com.veltro.inventory.dto.UpdateSupplierRequest;
import com.veltro.inventory.mapper.SupplierMapper;
import com.veltro.inventory.model.SupplierEntity;
import com.veltro.inventory.repository.SupplierRepository;
import com.veltro.inventory.exception.DuplicateResourceException;
import com.veltro.inventory.exception.NotFoundException;
import com.veltro.inventory.service.SupplierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SupplierService} (B2-04).
 *
 * <p>Tests supplier CRUD operations and tax ID uniqueness validation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierService")
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;
    
    @Mock
    private SupplierMapper supplierMapper;
    
    private SupplierService supplierService;
    
    private SupplierEntity supplierEntity;
    private SupplierResponse supplierResponse;
    private CreateSupplierRequest createRequest;
    private UpdateSupplierRequest updateRequest;

    @BeforeEach
    void setUp() {
        // Manual service instantiation
        supplierService = new SupplierService(supplierRepository, supplierMapper);
        
        supplierEntity = new SupplierEntity();
        supplierEntity.setId(1L);
        supplierEntity.setTaxId("12345678901");
        supplierEntity.setCompanyName("Test Supplier Corp");
        supplierEntity.setEmail("contact@testsupplier.com");
        supplierEntity.setActive(true);

        supplierResponse = new SupplierResponse(
                1L, "Test Supplier Corp", "12345678901", 
                "contact@testsupplier.com", "555-1234", 
                "123 Business St", "Notes", true
        );

        createRequest = new CreateSupplierRequest(
                "New Supplier Corp", "98765432109", 
                "new@supplier.com", "555-5678", 
                "456 Commerce Ave", "New supplier notes"
        );

        updateRequest = new UpdateSupplierRequest(
                "Updated Supplier Corp", "updated@supplier.com", 
                "555-9999", "789 Trade Blvd", "Updated notes"
        );
    }

    @Test
    @DisplayName("Should find all active suppliers")
    void shouldFindAllActiveSuppliers() {
        // Given
        when(supplierRepository.findAllByActiveTrueAndBusinessId(anyLong())).thenReturn(List.of(supplierEntity));
        when(supplierMapper.toResponse(supplierEntity)).thenReturn(supplierResponse);

        // When
        List<SupplierResponse> result = supplierService.findAll();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(supplierResponse);
        verify(supplierRepository, times(1)).findAllByActiveTrueAndBusinessId(anyLong());
        verify(supplierMapper, times(1)).toResponse(supplierEntity);
    }

    @Test
    @DisplayName("Should find supplier by ID")
    void shouldFindSupplierById() {
        // Given
        when(supplierRepository.findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong())).thenReturn(Optional.of(supplierEntity));
        when(supplierMapper.toResponse(supplierEntity)).thenReturn(supplierResponse);

        // When
        SupplierResponse result = supplierService.findById(1L);

        // Then
        assertThat(result).isEqualTo(supplierResponse);
        verify(supplierRepository, times(1)).findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong());
        verify(supplierMapper, times(1)).toResponse(supplierEntity);
    }

    @Test
    @DisplayName("Should throw NotFoundException when supplier ID not found")
    void shouldThrowNotFoundExceptionWhenSupplierIdNotFound() {
        // Given
        when(supplierRepository.findByIdAndActiveTrueAndBusinessId(eq(99L), anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> supplierService.findById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Supplier not found with id: 99");
    }

    @Test
    @DisplayName("Should find supplier by tax ID")
    void shouldFindSupplierByTaxId() {
        // Given
        String taxId = "12345678901";
        when(supplierRepository.findByTaxIdAndActiveTrueAndBusinessId(eq(taxId), anyLong()));
        when(supplierMapper.toResponse(supplierEntity)).thenReturn(supplierResponse);

        // When
        SupplierResponse result = supplierService.findByTaxId(taxId);

        // Then
        assertThat(result).isEqualTo(supplierResponse);
        verify(supplierRepository.findByTaxIdAndActiveTrueAndBusinessId(eq(taxId), anyLong()));
        verify(supplierMapper, times(1)).toResponse(supplierEntity);
    }

    @Test
    @DisplayName("Should create new supplier successfully")
    void shouldCreateNewSupplierSuccessfully() {
        // Given
        SupplierEntity newEntity = new SupplierEntity();
        when(supplierRepository.existsByTaxIdAndActiveTrueAndIdNotAndBusinessId(eq(createRequest.taxId()), eq(null), anyLong()));
        when(supplierMapper.toEntity(createRequest)).thenReturn(newEntity);
        when(supplierRepository.save(newEntity)).thenReturn(supplierEntity);
        when(supplierMapper.toResponse(supplierEntity)).thenReturn(supplierResponse);

        // When
        SupplierResponse result = supplierService.create(createRequest);

        // Then
        assertThat(result).isEqualTo(supplierResponse);
        verify(supplierRepository.existsByTaxIdAndActiveTrueAndIdNotAndBusinessId(eq(createRequest.taxId()), eq(null), anyLong()));
        verify(supplierMapper, times(1)).toEntity(createRequest);
        verify(supplierRepository, times(1)).save(newEntity);
        verify(supplierMapper, times(1)).toResponse(supplierEntity);
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when tax ID exists")
    void shouldThrowDuplicateResourceExceptionWhenTaxIdExists() {
        // Given
        when(supplierRepository.existsByTaxIdAndActiveTrueAndIdNotAndBusinessId(eq(createRequest.taxId()), eq(null), anyLong()));

        // When/Then
        assertThatThrownBy(() -> supplierService.create(createRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Supplier with tax ID '" + createRequest.taxId() + "' already exists");
    }

    @Test
    @DisplayName("Should update supplier successfully")
    void shouldUpdateSupplierSuccessfully() {
        // Given
        when(supplierRepository.findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong())).thenReturn(Optional.of(supplierEntity));
        when(supplierRepository.save(supplierEntity)).thenReturn(supplierEntity);
        when(supplierMapper.toResponse(supplierEntity)).thenReturn(supplierResponse);

        // When
        SupplierResponse result = supplierService.update(1L, updateRequest);

        // Then
        assertThat(result).isEqualTo(supplierResponse);
        verify(supplierRepository, times(1)).findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong());
        verify(supplierMapper, times(1)).updateEntity(updateRequest, supplierEntity);
        verify(supplierRepository, times(1)).save(supplierEntity);
        verify(supplierMapper, times(1)).toResponse(supplierEntity);
    }

    @Test
    @DisplayName("Should soft delete supplier successfully")
    void shouldSoftDeleteSupplierSuccessfully() {
        // Given
        when(supplierRepository.findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong())).thenReturn(Optional.of(supplierEntity));
        when(supplierRepository.save(supplierEntity)).thenReturn(supplierEntity);

        // When
        supplierService.delete(1L);

        // Then
        verify(supplierRepository, times(1)).findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong());
        verify(supplierRepository, times(1)).save(supplierEntity);
        // Note: setActive(false) call is tested through integration or by verifying the entity state
    }
}
