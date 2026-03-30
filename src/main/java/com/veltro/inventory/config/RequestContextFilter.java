package com.veltro.inventory.config;

import com.veltro.inventory.security.RequestContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that captures request context (like client IP) and makes it available
 * via {@link RequestContextHolder} throughout the request lifecycle.
 * 
 * <p>Runs early in the filter chain (before authentication) to ensure context
 * is available for all subsequent processing including audit logging.
 * 
 * <p>Handles proxy scenarios by checking {@code X-Forwarded-For} header first
 * (used by Heroku, nginx, and other reverse proxies).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Extract client IP considering proxy headers
            String clientIp = extractClientIp(request);
            
            // Set request context for this thread
            RequestContextHolder.set(new RequestContextHolder.RequestContext(clientIp));
            
            // Continue filter chain
            filterChain.doFilter(request, response);
        } finally {
            // Always clear context to prevent memory leaks
            RequestContextHolder.clear();
        }
    }

    /**
     * Extracts the real client IP address from the request.
     * 
     * <p>Checks {@code X-Forwarded-For} header first (for proxied requests like Heroku),
     * then falls back to {@code request.getRemoteAddr()}.
     * 
     * @param request the HTTP request
     * @return the client IP address
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs: "client, proxy1, proxy2"
            // Take the first one (original client IP)
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
