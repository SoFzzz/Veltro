package com.veltro.inventory.service;

import com.veltro.inventory.infrastructure.ai.ClipConfig;
import com.veltro.inventory.infrastructure.ai.HuggingFaceClipClient;
import com.veltro.inventory.infrastructure.ai.VectorUtils;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * Semantic search provider that delegates embedding generation to a remote
 * CLIP service hosted on Hugging Face Spaces. Takes precedence over
 * {@link LocalOnnxSearchProvider} when a remote URL is configured.
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class RemoteClipSearchProvider implements SemanticSearchProvider {

    private final HuggingFaceClipClient huggingFaceClipClient;
    private final ProductRepository productRepository;
    private final ClipConfig clipConfig;
    private final LocalOnnxSearchProvider localOnnxSearchProvider;

    @Override
    public boolean isModelLoaded() {
        if (isRemoteConfigured()) {
            return true;
        }
        return localOnnxSearchProvider.isModelLoaded();
    }

    @Override
    public String getModelVersion() {
        if (isRemoteConfigured()) {
            return clipConfig.getVersion() + "-remote";
        }
        return localOnnxSearchProvider.getModelVersion();
    }

    @Override
    public Optional<List<ProductEntity>> search(MultipartFile image, Long businessId, int limit) {
        if (isRemoteConfigured()) {
            return searchRemote(image, businessId, limit);
        }
        return localOnnxSearchProvider.search(image, businessId, limit);
    }

    private boolean isRemoteConfigured() {
        return clipConfig.getRemoteUrl() != null && !clipConfig.getRemoteUrl().isBlank();
    }

    private Optional<List<ProductEntity>> searchRemote(MultipartFile image, Long businessId, int limit) {
        try {
            Optional<float[]> embeddingOpt = huggingFaceClipClient.generateEmbedding(image);
            if (embeddingOpt.isEmpty()) {
                log.warn("Remote CLIP returned empty embedding; falling back to local provider.");
                return localOnnxSearchProvider.search(image, businessId, limit);
            }

            String embedding = VectorUtils.formatPgVector(embeddingOpt.get());
            return Optional.of(productRepository.findSimilarProducts(
                    embedding,
                    businessId,
                    clipConfig.getSimilarityThreshold(),
                    limit
            ));
        } catch (Exception e) {
            log.error("Remote CLIP search failed; falling back to local provider.", e);
            return localOnnxSearchProvider.search(image, businessId, limit);
        }
    }
}
