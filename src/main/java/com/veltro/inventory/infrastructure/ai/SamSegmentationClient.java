package com.veltro.inventory.infrastructure.ai;

import com.veltro.inventory.dto.scanner.SamSegmentationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SamSegmentationClient {

    private final RestTemplate restTemplate;

    @Value("${veltro.ai.sam.remote-url:}")
    private String samApiUrl;

    public boolean isAvailable() {
        return samApiUrl != null && !samApiUrl.isBlank();
    }

    public SamSegmentationResponse segmentWithAI(MultipartFile file, float x, float y) throws IOException {
        if (samApiUrl == null || samApiUrl.isBlank()) {
            log.error("La variable DIGITALOCEAN_SAM_URL no está configurada (veltro.ai.sam.remote-url).");
            throw new IllegalStateException("SAM API URL no configurada");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        ByteArrayResource fileAsResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg";
            }
        };

        body.add("file", fileAsResource);
        body.add("x", String.valueOf(x));
        body.add("y", String.valueOf(y));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<SamSegmentationResponse> response = restTemplate.postForEntity(
                    samApiUrl,
                    requestEntity,
                    SamSegmentationResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Error al conectar con la IA de SAM en DigitalOcean: {}", e.getMessage());
            throw new RuntimeException("Error en comunicación con servidor SAM", e);
        }
    }
}
