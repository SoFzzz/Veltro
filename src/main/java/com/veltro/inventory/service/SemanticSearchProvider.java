package com.veltro.inventory.service;

import com.veltro.inventory.model.ProductEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface SemanticSearchProvider {

    boolean isModelLoaded();

    String getModelVersion();

    Optional<List<ProductEntity>> search(MultipartFile image, Long businessId, int limit);
}
