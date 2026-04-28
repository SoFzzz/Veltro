package com.veltro.inventory.service;

import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.ProductRepository;
import com.veltro.inventory.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClipVectorSearchService implements VectorSearchService {

    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductEntity> findSimilarProducts(float[] embedding, int limit) {
        Long businessId = TenantContext.getBusinessId();
        
        // Convert float array to pgvector string format: "[val1,val2,...]"
        String embeddingStr = "[" + Arrays.toString(embedding).replace("[", "").replace("]", "").replace(" ", "") + "]";
        
        return productRepository.findSimilarProducts(embeddingStr, businessId, limit);
    }
}
