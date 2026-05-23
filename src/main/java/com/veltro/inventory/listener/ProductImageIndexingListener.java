package com.veltro.inventory.listener;

import com.veltro.inventory.event.ProductImageUploadedEvent;
import com.veltro.inventory.infrastructure.ai.ClipConfig;
import com.veltro.inventory.infrastructure.ai.ClipInferenceService;
import com.veltro.inventory.infrastructure.ai.HuggingFaceClipClient;
import com.veltro.inventory.infrastructure.ai.VectorUtils;
import com.veltro.inventory.model.IndexingStatus;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.ProductRepository;
import com.veltro.inventory.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Asynchronous listener that generates CLIP vector embeddings for uploaded product images.
 *
 * <p>Uses a two-tier strategy:
 * <ol>
 *   <li><strong>Local ONNX:</strong> preferred when the CLIP model is loaded in-process.</li>
 *   <li><strong>Remote API:</strong> fallback to the Hugging Face Spaces endpoint when
 *       the local model is unavailable (e.g., on resource-constrained PaaS like Heroku).</li>
 * </ol>
 *
 * <p>If neither provider can generate an embedding, the product is marked
 * {@link IndexingStatus#INDEXING_FAILED} with a descriptive error message.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductImageIndexingListener {

    private final ProductRepository productRepository;
    private final ClipInferenceService clipInferenceService;
    private final HuggingFaceClipClient huggingFaceClipClient;
    private final ClipConfig clipConfig;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductImageUploaded(ProductImageUploadedEvent event) {
        TenantContext.setOverride(event.businessId(), event.userId(), event.username());
        try {
            Optional<ProductEntity> productOpt =
                    productRepository.findByIdAndActiveTrueAndBusinessId(event.productId(), event.businessId());
            if (productOpt.isEmpty()) {
                log.warn("Product not found during async indexing. productId={}", event.productId());
                return;
            }

            ProductEntity product = productOpt.get();

            if (!isAnyProviderAvailable()) {
                log.warn("No CLIP provider available (local nor remote). productId={}", event.productId());
                product.setIndexingStatus(IndexingStatus.INDEXING_FAILED);
                product.setLastIndexingError("No CLIP embedding provider available. "
                        + "Local ONNX model not loaded and remote URL not configured.");
                productRepository.save(product);
                return;
            }

            try {
                String primaryEmbedding = generateEmbeddingFromPath(event.primaryFilePath());
                product.setEmbedding(primaryEmbedding);

                if (event.secondaryFilePath() != null) {
                    String secondaryEmbedding = generateEmbeddingFromPath(event.secondaryFilePath());
                    product.setEmbeddingSecondary(secondaryEmbedding);
                } else {
                    product.setEmbeddingSecondary(null);
                }

                product.setIndexingStatus(IndexingStatus.INDEXING_READY);
                product.setLastIndexingError(null);
                log.info("Product indexed successfully. productId={}", event.productId());
            } catch (Exception ex) {
                product.setIndexingStatus(IndexingStatus.INDEXING_FAILED);
                product.setLastIndexingError(ex.getMessage());
                log.error("Async image indexing failed for product {}", event.productId(), ex);
            }

            productRepository.save(product);
        } finally {
            TenantContext.clearOverride();
        }
    }

    /**
     * Reads an image from disk and generates a formatted pgvector embedding string.
     * Tries local ONNX first; falls back to the remote Hugging Face endpoint.
     */
    private String generateEmbeddingFromPath(Path imagePath) throws Exception {
        if (clipInferenceService.isModelLoaded()) {
            return generateLocalEmbedding(imagePath);
        }

        log.info("Local ONNX model not loaded; delegating to remote CLIP provider. path={}", imagePath);
        return generateRemoteEmbedding(imagePath);
    }

    private String generateLocalEmbedding(Path imagePath) throws Exception {
        try (InputStream is = Files.newInputStream(imagePath)) {
            BufferedImage bufferedImage = ImageIO.read(is);
            if (bufferedImage == null) {
                throw new IllegalStateException("Could not read image format from: " + imagePath);
            }

            Optional<float[]> embeddingOpt = clipInferenceService.generateEmbedding(bufferedImage);
            if (embeddingOpt.isEmpty()) {
                throw new IllegalStateException("Local ONNX model inference returned empty for: " + imagePath);
            }
            return VectorUtils.formatPgVector(embeddingOpt.get());
        }
    }

    private String generateRemoteEmbedding(Path imagePath) throws Exception {
        byte[] imageBytes = Files.readAllBytes(imagePath);
        String filename = imagePath.getFileName().toString();

        Optional<float[]> embeddingOpt = huggingFaceClipClient.generateEmbedding(imageBytes, filename);
        if (embeddingOpt.isEmpty()) {
            throw new IllegalStateException("Remote CLIP service returned empty embedding for: " + imagePath);
        }
        return VectorUtils.formatPgVector(embeddingOpt.get());
    }

    private boolean isAnyProviderAvailable() {
        if (clipInferenceService.isModelLoaded()) {
            return true;
        }
        return clipConfig.getRemoteUrl() != null && !clipConfig.getRemoteUrl().isBlank();
    }
}
