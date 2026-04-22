package com.veltro.inventory.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean
    public Bandwidth loginRateLimit() {
        return Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
    }
}
