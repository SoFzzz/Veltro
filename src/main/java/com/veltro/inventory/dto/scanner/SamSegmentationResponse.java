package com.veltro.inventory.dto.scanner;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SamSegmentationResponse(
        String estado,
        @JsonProperty("mascara_base64") String mascaraBase64
) {}
