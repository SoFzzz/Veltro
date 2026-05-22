package com.veltro.inventory.service;

import com.veltro.inventory.dto.inventory.AlertConfigurationResponse;
import com.veltro.inventory.dto.inventory.UpdateAlertConfigurationRequest;
import com.veltro.inventory.mapper.AlertConfigurationMapper;
import com.veltro.inventory.model.AlertConfigurationEntity;
import com.veltro.inventory.model.InventoryEntity;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.AlertConfigurationRepository;
import com.veltro.inventory.repository.InventoryRepository;
import com.veltro.inventory.security.TenantProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertConfigurationServiceTest {

    private static final Long BUSINESS_ID = 100L;
    private static final Long PRODUCT_ID = 1L;

    @Mock private AlertConfigurationRepository configurationRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private AlertConfigurationMapper configurationMapper;
    @Mock private TenantProvider tenantProvider;
    @Mock private AlertService alertService;

    @InjectMocks
    private AlertConfigurationService service;

    private InventoryEntity createInventory(Integer minStock, Integer maxStock) {
        InventoryEntity inv = new InventoryEntity();
        ProductEntity product = new ProductEntity();
        product.setId(PRODUCT_ID);
        inv.setProduct(product);
        inv.setMinStock(minStock);
        inv.setMaxStock(maxStock);
        inv.setBusinessId(BUSINESS_ID);
        return inv;
    }

    @Test
    @DisplayName("getConfiguration — configuración existente retorna respuesta mapeada")
    void getConfiguration_existingConfig_returnsResponse() {
        when(tenantProvider.getBusinessId()).thenReturn(BUSINESS_ID);
        AlertConfigurationEntity config = new AlertConfigurationEntity();
        config.setId(1L);
        AlertConfigurationResponse expected = new AlertConfigurationResponse(PRODUCT_ID, 0, 5, 100);

        when(configurationRepository.findByProductIdAndActiveTrueAndBusinessId(PRODUCT_ID, BUSINESS_ID))
                .thenReturn(Optional.of(config));
        when(configurationMapper.toResponse(config)).thenReturn(expected);

        AlertConfigurationResponse result = service.getConfiguration(PRODUCT_ID);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("getConfiguration — sin configuración crea default y retorna")
    void getConfiguration_noConfig_createsDefault() {
        when(tenantProvider.getBusinessId()).thenReturn(BUSINESS_ID);
        InventoryEntity inventory = createInventory(10, 200);
        AlertConfigurationEntity savedConfig = new AlertConfigurationEntity();
        AlertConfigurationResponse expected = new AlertConfigurationResponse(PRODUCT_ID, 0, 10, 200);

        when(configurationRepository.findByProductIdAndActiveTrueAndBusinessId(PRODUCT_ID, BUSINESS_ID))
                .thenReturn(Optional.empty());
        when(inventoryRepository.findByProductIdAndActiveTrueAndBusinessId(PRODUCT_ID, BUSINESS_ID))
                .thenReturn(Optional.of(inventory));
        when(configurationRepository.save(any(AlertConfigurationEntity.class))).thenReturn(savedConfig);
        when(configurationMapper.toResponse(savedConfig)).thenReturn(expected);

        AlertConfigurationResponse result = service.getConfiguration(PRODUCT_ID);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("getConfiguration — sin inventario lanza IllegalStateException")
    void getConfiguration_noInventory_throwsIllegalState() {
        when(tenantProvider.getBusinessId()).thenReturn(BUSINESS_ID);
        when(configurationRepository.findByProductIdAndActiveTrueAndBusinessId(PRODUCT_ID, BUSINESS_ID))
                .thenReturn(Optional.empty());
        when(inventoryRepository.findByProductIdAndActiveTrueAndBusinessId(PRODUCT_ID, BUSINESS_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getConfiguration(PRODUCT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Inventory not found");
    }

    @Test
    @DisplayName("updateConfiguration — configuración existente actualiza valores")
    void updateConfiguration_existingConfig_updatesValues() {
        when(tenantProvider.getBusinessId()).thenReturn(BUSINESS_ID);
        UpdateAlertConfigurationRequest request = new UpdateAlertConfigurationRequest(2, 10, 150);
        AlertConfigurationEntity config = new AlertConfigurationEntity();
        config.setId(1L);
        config.setCriticalStock(0);
        config.setMinStock(5);
        config.setOverstockThreshold(100);
        AlertConfigurationResponse expected = new AlertConfigurationResponse(PRODUCT_ID, 2, 10, 150);

        when(configurationRepository.findByProductIdAndActiveTrueAndBusinessId(PRODUCT_ID, BUSINESS_ID))
                .thenReturn(Optional.of(config));
        when(configurationRepository.save(config)).thenReturn(config);
        when(configurationMapper.toResponse(config)).thenReturn(expected);

        AlertConfigurationResponse result = service.updateConfiguration(PRODUCT_ID, request);

        assertThat(result).isEqualTo(expected);
        assertThat(config.getCriticalStock()).isEqualTo(2);
        assertThat(config.getMinStock()).isEqualTo(10);
        assertThat(config.getOverstockThreshold()).isEqualTo(150);
    }

    @Test
    @DisplayName("updateConfiguration — sin configuración crea default luego actualiza")
    void updateConfiguration_noConfig_createsDefaultThenUpdates() {
        when(tenantProvider.getBusinessId()).thenReturn(BUSINESS_ID);
        UpdateAlertConfigurationRequest request = new UpdateAlertConfigurationRequest(1, 8, 200);
        InventoryEntity inventory = createInventory(5, 100);
        AlertConfigurationEntity savedDefault = new AlertConfigurationEntity();
        savedDefault.setCriticalStock(0);
        savedDefault.setMinStock(5);
        savedDefault.setOverstockThreshold(100);
        AlertConfigurationResponse expected = new AlertConfigurationResponse(PRODUCT_ID, 1, 8, 200);

        when(configurationRepository.findByProductIdAndActiveTrueAndBusinessId(PRODUCT_ID, BUSINESS_ID))
                .thenReturn(Optional.empty());
        when(inventoryRepository.findByProductIdAndActiveTrueAndBusinessId(PRODUCT_ID, BUSINESS_ID))
                .thenReturn(Optional.of(inventory));
        when(configurationRepository.save(any(AlertConfigurationEntity.class))).thenReturn(savedDefault);
        when(configurationMapper.toResponse(any())).thenReturn(expected);

        AlertConfigurationResponse result = service.updateConfiguration(PRODUCT_ID, request);

        assertThat(result).isEqualTo(expected);
        verify(configurationRepository, org.mockito.Mockito.times(2)).save(any(AlertConfigurationEntity.class));
    }

    @Test
    @DisplayName("createDefault — usa minStock del inventario cuando es > 0")
    void createDefaultConfiguration_usesInventoryMinStock() {
        when(tenantProvider.getBusinessId()).thenReturn(BUSINESS_ID);
        InventoryEntity inventory = createInventory(15, 300);

        when(configurationRepository.findByProductIdAndActiveTrueAndBusinessId(PRODUCT_ID, BUSINESS_ID))
                .thenReturn(Optional.empty());
        when(inventoryRepository.findByProductIdAndActiveTrueAndBusinessId(PRODUCT_ID, BUSINESS_ID))
                .thenReturn(Optional.of(inventory));
        when(configurationRepository.save(any(AlertConfigurationEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(configurationMapper.toResponse(any())).thenReturn(new AlertConfigurationResponse(PRODUCT_ID, 0, 15, 300));

        service.getConfiguration(PRODUCT_ID);

        ArgumentCaptor<AlertConfigurationEntity> captor = ArgumentCaptor.forClass(AlertConfigurationEntity.class);
        verify(configurationRepository).save(captor.capture());
        assertThat(captor.getValue().getMinStock()).isEqualTo(15);
        assertThat(captor.getValue().getOverstockThreshold()).isEqualTo(300);
    }

    @Test
    @DisplayName("createDefault — usa defaults cuando inventario tiene minStock null")
    void createDefaultConfiguration_usesDefaultWhenInventoryMinStockNull() {
        when(tenantProvider.getBusinessId()).thenReturn(BUSINESS_ID);
        InventoryEntity inventory = createInventory(null, null);

        when(configurationRepository.findByProductIdAndActiveTrueAndBusinessId(PRODUCT_ID, BUSINESS_ID))
                .thenReturn(Optional.empty());
        when(inventoryRepository.findByProductIdAndActiveTrueAndBusinessId(PRODUCT_ID, BUSINESS_ID))
                .thenReturn(Optional.of(inventory));
        when(configurationRepository.save(any(AlertConfigurationEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(configurationMapper.toResponse(any())).thenReturn(new AlertConfigurationResponse(PRODUCT_ID, 0, 5, 100));

        service.getConfiguration(PRODUCT_ID);

        ArgumentCaptor<AlertConfigurationEntity> captor = ArgumentCaptor.forClass(AlertConfigurationEntity.class);
        verify(configurationRepository).save(captor.capture());
        assertThat(captor.getValue().getMinStock()).isEqualTo(5);
        assertThat(captor.getValue().getOverstockThreshold()).isEqualTo(100);
    }
}
