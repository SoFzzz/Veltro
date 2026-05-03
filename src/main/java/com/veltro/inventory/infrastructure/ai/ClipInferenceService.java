package com.veltro.inventory.infrastructure.ai;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.SessionOptions.OptLevel;
import com.veltro.inventory.exception.ai.ClipModelInferenceException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClipInferenceService {

    private final ClipConfig config;
    private final ClipImagePreprocessor preprocessor;

    private OrtEnvironment env;
    private OrtSession session;
    private boolean modelLoaded = false;

    @PostConstruct
    public void init() {
        if (!config.isEnabled()) {
            log.info("CLIP AI is disabled in configuration.");
            return;
        }

        try {
            this.env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setOptimizationLevel(OptLevel.BASIC_OPT);

            ClassPathResource modelResource = new ClassPathResource(config.getModelPath());
            if (modelResource.exists()) {
                Path tempModelFile = Files.createTempFile("clip-image-vit", ".onnx");
                try (InputStream is = modelResource.getInputStream()) {
                    Files.copy(is, tempModelFile, StandardCopyOption.REPLACE_EXISTING);
                }

                if (config.getModelChecksum() != null && !config.getModelChecksum().isBlank()) {
                    validateChecksum(tempModelFile, config.getModelChecksum());
                }

                this.session = env.createSession(tempModelFile.toString(), opts);
                this.modelLoaded = true;
                log.info("ONNX CLIP Model loaded successfully. Version: {}", config.getVersion());
            } else {
                log.warn("ONNX CLIP Model not found at classpath:{}. Inference will not be available.",
                        config.getModelPath());
            }
        } catch (Exception e) {
            log.error("Failed to initialize ONNX Runtime session for CLIP", e);
            this.modelLoaded = false;
        }
    }

    private void validateChecksum(Path file, String expectedChecksum) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(Files.readAllBytes(file));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        String actualChecksum = hexString.toString();
        if (!actualChecksum.equalsIgnoreCase(expectedChecksum)) {
            throw new IllegalStateException(
                    "Model checksum validation failed. Expected: " + expectedChecksum + " Actual: " + actualChecksum);
        }
    }

    public boolean isModelLoaded() {
        return modelLoaded;
    }

    public String getModelVersion() {
        return config.getVersion();
    }

    public Optional<float[]> generateEmbedding(BufferedImage image) {
        if (!modelLoaded) {
            return Optional.empty();
        }

        try {
            float[] preprocessedData = preprocessor.preprocess(image);

            // Create ONNX Tensor: shape is [1, 3, 224, 224]
            long[] shape = new long[] { 1, 3, 224, 224 };

            try (OnnxTensor tensor = OnnxTensor.createTensor(env, java.nio.FloatBuffer.wrap(preprocessedData), shape)) {
                // Determine input name dynamically
                String inputName = session.getInputNames().iterator().next();

                try (OrtSession.Result results = session.run(Collections.singletonMap(inputName, tensor))) {
                    float[][] output = (float[][]) results.get(0).getValue();
                    float[] embedding = output[0];
                    return Optional.of(normalizeL2(embedding));
                }
            }
        } catch (OrtException e) {
            throw new ClipModelInferenceException("ONNX Runtime error during inference", e);
        } catch (Exception e) {
            throw new ClipModelInferenceException("Unexpected error generating embedding", e);
        }
    }

    private float[] normalizeL2(float[] vector) {
        float sum = 0;
        for (float v : vector) {
            sum += v * v;
        }
        float magnitude = (float) Math.sqrt(sum);
        if (magnitude == 0)
            return vector;

        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / magnitude;
        }
        return normalized;
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (session != null) {
                session.close();
            }
            if (env != null) {
                env.close();
            }
        } catch (OrtException e) {
            log.error("Error closing ONNX Runtime resources", e);
        }
    }
}
