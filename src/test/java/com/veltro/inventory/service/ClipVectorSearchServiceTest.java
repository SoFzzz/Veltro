package com.veltro.inventory.service;

import com.veltro.inventory.infrastructure.ai.ClipConfig;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.ProductRepository;
import com.veltro.inventory.security.TenantProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClipVectorSearchServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private TenantProvider tenantProvider;
    @Mock private ClipConfig clipConfig;

    @InjectMocks
    private ClipVectorSearchService service;

    @Test
    @DisplayName("findSimilarProducts — delega al repositorio con parámetros correctos")
    void findSimilarProducts_delegatesToRepository() {
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};
        ProductEntity product = new ProductEntity();
        product.setId(1L);

        when(tenantProvider.getBusinessId()).thenReturn(100L);
        when(clipConfig.getSimilarityThreshold()).thenReturn(0.7);
        when(productRepository.findSimilarProducts(anyString(), eq(100L), eq(0.7), eq(5)))
                .thenReturn(List.of(product));

        List<ProductEntity> result = service.findSimilarProducts(embedding, 5);

        assertThat(result).hasSize(1);
        verify(productRepository).findSimilarProducts(anyString(), eq(100L), eq(0.7), eq(5));
    }

    @Test
    @DisplayName("findSimilarProducts — usa businessId del tenant")
    void findSimilarProducts_usesCorrectBusinessId() {
        when(tenantProvider.getBusinessId()).thenReturn(42L);
        when(clipConfig.getSimilarityThreshold()).thenReturn(0.5);
        when(productRepository.findSimilarProducts(anyString(), anyLong(), anyDouble(), anyInt()))
                .thenReturn(List.of());

        service.findSimilarProducts(new float[]{0.1f}, 10);

        verify(tenantProvider).getBusinessId();
        verify(productRepository).findSimilarProducts(anyString(), eq(42L), anyDouble(), anyInt());
    }

    @Test
    @DisplayName("findSimilarProducts — usa threshold de ClipConfig")
    void findSimilarProducts_usesCorrectThreshold() {
        when(tenantProvider.getBusinessId()).thenReturn(1L);
        when(clipConfig.getSimilarityThreshold()).thenReturn(0.85);
        when(productRepository.findSimilarProducts(anyString(), anyLong(), anyDouble(), anyInt()))
                .thenReturn(List.of());

        service.findSimilarProducts(new float[]{0.1f}, 3);

        verify(productRepository).findSimilarProducts(anyString(), anyLong(), eq(0.85), eq(3));
    }

    @Test
    @DisplayName("findSimilarProducts — resultado vacío devuelve lista vacía")
    void findSimilarProducts_emptyResult() {
        when(tenantProvider.getBusinessId()).thenReturn(1L);
        when(clipConfig.getSimilarityThreshold()).thenReturn(0.7);
        when(productRepository.findSimilarProducts(anyString(), anyLong(), anyDouble(), anyInt()))
                .thenReturn(List.of());

        List<ProductEntity> result = service.findSimilarProducts(new float[]{0.5f}, 10);
        assertThat(result).isEmpty();
    }
}
