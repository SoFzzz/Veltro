package com.veltro.inventory.infrastructure.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "veltro.ai.clip")
@Data
public class ClipConfig {
    private boolean enabled = true;
    private String modelPath = "models/clip-image-vit-32.onnx";
    private String modelChecksum = ""; // Optional, if empty no checksum is validated
    private int embeddingDimension = 512;
    private double similarityThreshold = 0.45;
    private double confidenceThreshold = 0.75;
    private String version = "ViT-B/32-v1";
    private String remoteUrl = ""; // Hugging Face Space URL (e.g. https://veltro-ucc-veltro-models.hf.space)
}
