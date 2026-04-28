package com.veltro.inventory.dto.scanner;

import com.veltro.inventory.dto.catalog.ProductResponse;
import java.util.List;

public record AiDetectionResponse(
        String id,
        double x,
        double y,
        double width,
        double height,
        String className,
        double confidence,
        List<ProductResponse> matches
) {}
