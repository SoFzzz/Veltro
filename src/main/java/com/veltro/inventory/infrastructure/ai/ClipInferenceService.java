package com.veltro.inventory.infrastructure.ai;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.SessionOptions.OptLevel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class ClipInferenceService {
    private OrtEnvironment env;
    private OrtSession session;

    @PostConstruct
    public void init() {
        try {
            // Cargar entorno y sesión globalmente al inicio
            this.env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setOptimizationLevel(OptLevel.BASIC_OPT); // Evitar optimizaciones agresivas en Heroku

            ClassPathResource modelResource = new ClassPathResource("models/clip-image-vit-32.onnx");
            if (modelResource.exists()) {
                // OrtSession requires a file path or byte array. We can copy to a temp file.
                Path tempModelFile = Files.createTempFile("clip-image-vit-32", ".onnx");
                try (InputStream is = modelResource.getInputStream()) {
                    Files.copy(is, tempModelFile, StandardCopyOption.REPLACE_EXISTING);
                }
                this.session = env.createSession(tempModelFile.toString(), opts);
                log.info("ONNX CLIP Model loaded successfully.");
            } else {
                log.warn("ONNX CLIP Model not found at classpath:models/clip-image-vit-32.onnx. Inference will not be available.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize ONNX Runtime session", e);
        }
    }

    
    // The actual inference logic would go here, taking an image, preprocessing it, and calling session.run()
}
