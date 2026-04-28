package com.veltro.inventory.service;

import com.veltro.inventory.model.ProductEntity;
import java.util.List;

public interface VectorSearchService {

    /**
     * Finds products in the catalog that visually match the given image embedding.
     */
    List<ProductEntity> findSimilarProducts(float[] embedding, int limit);
}
