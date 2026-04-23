package com.veltro.inventory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veltro.inventory.dto.common.ErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";

    private final Bandwidth loginRateLimit;
    private final ObjectMapper objectMapper;
    private final Map<String, Bucket> bucketsByClientIp = new ConcurrentHashMap<>();

    public LoginRateLimitFilter(Bandwidth loginRateLimit, ObjectMapper objectMapper) {
        this.loginRateLimit = loginRateLimit;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod()) && LOGIN_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String clientIp = extractClientIp(request);
        Bucket bucket = bucketsByClientIp.computeIfAbsent(clientIp,
                ignored -> Bucket.builder().addLimit(loginRateLimit).build());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Login rate limit exceeded for IP {} on {}", clientIp, request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.of(
                "RATE_LIMIT_EXCEEDED",
                "Too many login attempts. Please try again in a minute.",
                HttpStatus.TOO_MANY_REQUESTS,
                request.getRequestURI());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
