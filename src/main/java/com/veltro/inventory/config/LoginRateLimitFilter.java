package com.veltro.inventory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veltro.inventory.dto.common.ErrorResponse;
import com.veltro.inventory.security.RequestContextHolder;
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
    private static final String RATE_LIMIT_MESSAGE =
            "Demasiados intentos de inicio de sesión. Por favor, intente de nuevo en un minuto.";

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

        String clientIp = RequestContextHolder.getClientIp();
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
        }
        Bucket bucket = bucketsByClientIp.computeIfAbsent(clientIp,
                ignored -> Bucket.builder().addLimit(loginRateLimit).build());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Login rate limit exceeded for IP {} on {}", clientIp, request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.of(
                "RATE_LIMIT_EXCEEDED",
                RATE_LIMIT_MESSAGE,
                HttpStatus.TOO_MANY_REQUESTS,
                request.getRequestURI());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

}
