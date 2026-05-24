package com.veltro.inventory.dto.scanner;

import java.util.List;

public record DetectSearchResponse(
    List<SemanticSearchMatchDto> matches
) {}
