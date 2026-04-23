package com.veltro.inventory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.veltro.inventory.dto.common.ErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoginRateLimitFilter")
class LoginRateLimitFilterTest {

    private LoginRateLimitFilter filter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        Bandwidth loginRateLimit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        filter = new LoginRateLimitFilter(loginRateLimit, objectMapper);
    }

    @Test
    @DisplayName("Allows up to five login attempts per client IP")
    void allowsUpToFiveLoginAttemptsPerClientIp() throws Exception {
        for (int attempt = 1; attempt <= 5; attempt++) {
            MockHttpServletRequest request = loginRequest("192.168.1.10");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(chain.getRequest()).isNotNull();
        }
    }

    @Test
    @DisplayName("Blocks the sixth login attempt from the same client IP with 429")
    void blocksSixthLoginAttemptFromSameIp() throws Exception {
        for (int attempt = 1; attempt <= 5; attempt++) {
            filter.doFilter(loginRequest("10.0.0.5"), new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletRequest blockedRequest = loginRequest("10.0.0.5");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        MockFilterChain blockedChain = new MockFilterChain();

        filter.doFilter(blockedRequest, blockedResponse, blockedChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(blockedChain.getRequest()).isNull();

        ErrorResponse payload = objectMapper.readValue(blockedResponse.getContentAsByteArray(), ErrorResponse.class);
        assertThat(payload.code()).isEqualTo("RATE_LIMIT_EXCEEDED");
        assertThat(payload.message()).isEqualTo("Too many login attempts. Please try again in a minute.");
        assertThat(payload.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(payload.path()).isEqualTo("/api/v1/auth/login");
        assertThat(payload.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Uses independent buckets for different client IPs")
    void usesIndependentBucketsPerClientIp() throws Exception {
        for (int attempt = 1; attempt <= 5; attempt++) {
            filter.doFilter(loginRequest("172.16.0.1"), new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletRequest otherIpRequest = loginRequest("172.16.0.2");
        MockHttpServletResponse otherIpResponse = new MockHttpServletResponse();
        MockFilterChain otherIpChain = new MockFilterChain();

        filter.doFilter(otherIpRequest, otherIpResponse, otherIpChain);

        assertThat(otherIpResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(otherIpChain.getRequest()).isNotNull();
    }

    private MockHttpServletRequest loginRequest(String clientIp) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr(clientIp);
        return request;
    }
}
