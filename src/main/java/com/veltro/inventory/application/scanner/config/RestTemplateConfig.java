package com.veltro.inventory.application.scanner.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for REST client used in scanner module (B3-01).
 *
 * <p>Provides pre-configured beans for OpenAI Vision API calls
 * with appropriate timeouts and error handling.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Creates a RestTemplate bean configured for OpenAI Vision API calls.
     *
     * @return configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);   // 30 seconds connect timeout
        factory.setReadTimeout(180000);     // 180 seconds (3 min) for slow vision models
        
        return new RestTemplate(new BufferingClientHttpRequestFactory(factory));
    }

    /**
     * Creates an ObjectMapper bean for JSON parsing.
     *
     * @return ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
