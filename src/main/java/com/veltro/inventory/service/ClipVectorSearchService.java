package com.veltro.inventory.service;

import com.veltro.inventory.infrastructure.ai.ClipConfig;
import com.veltro.inventory.infrastructure.ai.VectorUtils;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.ProductRepository;
import com.veltro.inventory.security.TenantProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClipVectorSearchService implements VectorSearchService {

    private final ProductRepository productRepository;
    private final TenantProvider tenantProvider;
    private final ClipConfig clipConfig;

    @Override
    @Transactional(readOnly = true)
    public List<ProductEntity> findSimilarProducts(float[] embedding, int limit) {
        Long businessId = tenantProvider.getBusinessId();
        String embeddingStr = VectorUtils.formatPgVector(embedding);
        return productRepository.findSimilarProducts(
                embeddingStr,
                businessId,
                clipConfig.getSimilarityThreshold(),
                limit
        );
    }
}
