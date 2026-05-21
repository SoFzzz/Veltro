package com.veltro.inventory.listener;

import com.veltro.inventory.event.ProductImageUploadedEvent;
import com.veltro.inventory.infrastructure.ai.ClipInferenceService;
import com.veltro.inventory.model.IndexingStatus;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.ProductRepository;
import com.veltro.inventory.security.TenantContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductImageIndexingListenerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ClipInferenceService clipInferenceService;

    @Test
    @DisplayName("listener sets INDEXING_READY on successful embedding generation")
    void onProductImageUploaded_success_setsReady() throws Exception {
        ProductEntity entity = new ProductEntity();
        entity.setId(1L);
        entity.setBusinessId(100L);
        entity.setActive(true);

        Path image = Files.createTempFile("veltro-listener-", ".png");
        Files.write(image, pngBytes());

        when(productRepository.findByIdAndActiveTrueAndBusinessId(1L, 100L)).thenReturn(Optional.of(entity));
        when(clipInferenceService.isModelLoaded()).thenReturn(true);
        when(clipInferenceService.generateEmbedding(any())).thenReturn(Optional.of(new float[] {1.0f, 2.0f}));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductImageIndexingListener listener = new ProductImageIndexingListener(productRepository, clipInferenceService);
        listener.onProductImageUploaded(new ProductImageUploadedEvent(1L, 100L, 77L, "user", image, null));

        assertThat(entity.getIndexingStatus()).isEqualTo(IndexingStatus.INDEXING_READY);
        assertThat(entity.getEmbedding()).isEqualTo("[1.0,2.0]");
        assertThat(TenantContext.getOptionalUsername()).isEmpty();
        verify(productRepository).save(entity);
    }

    @Test
    @DisplayName("listener sets INDEXING_FAILED when inference fails and clears tenant context")
    void onProductImageUploaded_failure_setsFailedAndClearsContext() throws Exception {
        ProductEntity entity = new ProductEntity();
        entity.setId(1L);
        entity.setBusinessId(100L);
        entity.setActive(true);

        Path image = Files.createTempFile("veltro-listener-", ".png");
        Files.write(image, pngBytes());

        when(productRepository.findByIdAndActiveTrueAndBusinessId(1L, 100L)).thenReturn(Optional.of(entity));
        when(clipInferenceService.isModelLoaded()).thenReturn(true);
        when(clipInferenceService.generateEmbedding(any())).thenThrow(new RuntimeException("boom"));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductImageIndexingListener listener = new ProductImageIndexingListener(productRepository, clipInferenceService);
        listener.onProductImageUploaded(new ProductImageUploadedEvent(1L, 100L, 77L, "user", image, null));

        assertThat(entity.getIndexingStatus()).isEqualTo(IndexingStatus.INDEXING_FAILED);
        assertThat(entity.getLastIndexingError()).contains("boom");
        assertThat(TenantContext.getOptionalUsername()).isEmpty();
    }

    private byte[] pngBytes() {
        try {
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}
