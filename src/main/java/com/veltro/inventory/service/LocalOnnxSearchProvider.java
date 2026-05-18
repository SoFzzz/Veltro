package com.veltro.inventory.service;

import com.veltro.inventory.infrastructure.ai.ClipConfig;
import com.veltro.inventory.infrastructure.ai.ClipInferenceService;
import com.veltro.inventory.infrastructure.ai.VectorUtils;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalOnnxSearchProvider implements SemanticSearchProvider {

    private final ClipInferenceService clipInferenceService;
    private final ProductRepository productRepository;
    private final ClipConfig clipConfig;

    @Override
    public boolean isModelLoaded() {
        return clipInferenceService.isModelLoaded();
    }

    @Override
    public String getModelVersion() {
        return clipInferenceService.getModelVersion();
    }

    @Override
    public Optional<List<ProductEntity>> search(MultipartFile image, Long businessId, int limit) {
        try (InputStream inputStream = image.getInputStream()) {
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            if (bufferedImage == null) {
                return Optional.empty();
            }

            Optional<float[]> embeddingOpt = clipInferenceService.generateEmbedding(bufferedImage);
            if (embeddingOpt.isEmpty()) {
                return Optional.empty();
            }

            String embedding = VectorUtils.formatPgVector(embeddingOpt.get());
            return Optional.of(productRepository.findSimilarProducts(
                    embedding,
                    businessId,
                    clipConfig.getSimilarityThreshold(),
                    limit
            ));
        } catch (Exception e) {
            log.error("Semantic search provider failed", e);
            return Optional.empty();
        }
    }
}
