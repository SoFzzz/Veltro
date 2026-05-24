package com.veltro.inventory.service;

import com.veltro.inventory.infrastructure.ai.ClipConfig;
import com.veltro.inventory.infrastructure.ai.ClipInferenceService;
import com.veltro.inventory.infrastructure.ai.HuggingFaceClipClient;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchIndexingServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ClipInferenceService clipInferenceService;
    @Mock private HuggingFaceClipClient huggingFaceClipClient;
    @Mock private ClipConfig clipConfig;

    private BatchIndexingService service;

    @BeforeEach
    void setUp() {
        service = new BatchIndexingService(productRepository, clipInferenceService, huggingFaceClipClient, clipConfig);
        ReflectionTestUtils.setField(service, "uploadsDirPath", "./uploads");
    }

    @Test
    @DisplayName("reindexAll — llama a findAll del repositorio")
    void reindexAll_callsFindAll() {
        when(clipInferenceService.isModelLoaded()).thenReturn(true);
        when(productRepository.findAll()).thenReturn(List.of(new ProductEntity()));

        service.reindexAll();

        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("reindexAll — no lanza excepción con lista vacía")
    void reindexAll_doesNotThrow_emptyList() {
        when(clipInferenceService.isModelLoaded()).thenReturn(true);
        when(productRepository.findAll()).thenReturn(List.of());

        assertThatCode(() -> service.reindexAll())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("reindexAll — maneja múltiples productos")
    void reindexAll_handlesMultipleProducts() {
        ProductEntity p1 = new ProductEntity();
        p1.setId(1L);
        ProductEntity p2 = new ProductEntity();
        p2.setId(2L);
        when(clipInferenceService.isModelLoaded()).thenReturn(true);
        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        service.reindexAll();

        verify(productRepository).findAll();
    }
}
