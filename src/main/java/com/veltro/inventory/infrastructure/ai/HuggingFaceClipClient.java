package com.veltro.inventory.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP client for the remote CLIP embedding service hosted on Hugging Face Spaces.
 * Sends images to the {@code /embed-image} endpoint and returns the raw float embedding.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HuggingFaceClipClient {

    private final RestTemplate restTemplate;
    private final ClipConfig clipConfig;

    /**
     * Sends an image to the remote CLIP service and retrieves its embedding vector.
     *
     * @param image the uploaded image file
     * @return the embedding as a float array, or empty if the call fails
     */
    public Optional<float[]> generateEmbedding(MultipartFile image) {
        try {
            String filename = image.getOriginalFilename() != null
                    ? image.getOriginalFilename()
                    : "image.jpg";
            return generateEmbedding(image.getBytes(), filename);
        } catch (Exception e) {
            log.error("Failed to read bytes from MultipartFile", e);
            return Optional.empty();
        }
    }

    /**
     * Sends raw image bytes to the remote CLIP service and retrieves the embedding vector.
     * This overload is used by the indexing pipeline where images are read from disk.
     *
     * @param imageBytes raw image content
     * @param filename   descriptive filename for the multipart upload
     * @return the embedding as a float array, or empty if the call fails
     */
    public Optional<float[]> generateEmbedding(byte[] imageBytes, String filename) {
        String remoteUrl = clipConfig.getRemoteUrl();
        if (remoteUrl == null || remoteUrl.isBlank()) {
            log.warn("CLIP remote URL is not configured; skipping remote embedding.");
            return Optional.empty();
        }

        String endpoint = remoteUrl.endsWith("/")
                ? remoteUrl + "embed-image"
                : remoteUrl + "/embed-image";

        try {
            ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", imageResource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                    endpoint, request, (Class<Map<String, Object>>) (Class<?>) Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("CLIP remote service returned non-OK status: {}", response.getStatusCode());
                return Optional.empty();
            }

            Object embeddingRaw = response.getBody().get("embedding");
            if (!(embeddingRaw instanceof List<?> embeddingList)) {
                log.error("CLIP remote response missing 'embedding' field or wrong type.");
                return Optional.empty();
            }

            float[] embedding = new float[embeddingList.size()];
            for (int i = 0; i < embeddingList.size(); i++) {
                embedding[i] = ((Number) embeddingList.get(i)).floatValue();
            }

            log.debug("Remote CLIP embedding generated: {} dimensions", embedding.length);
            return Optional.of(embedding);

        } catch (Exception e) {
            log.error("Failed to call remote CLIP service at {}", endpoint, e);
            return Optional.empty();
        }
    }
}
