package com.veltro.inventory.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        if (!jwtTokenProvider.isValidAccessToken(token)) {
            log.debug("Invalid or expired access token on {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // Only authenticate if no authentication is already set in the context
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String username = jwtTokenProvider.extractUsername(token);
            String role = jwtTokenProvider.extractRole(token);
            Long businessId = jwtTokenProvider.extractBusinessId(token);
            Long userId = jwtTokenProvider.extractUserId(token);

            // Build VeltroUserDetails from JWT claims — avoids DB hit per request
            UserDetails userDetails = new VeltroUserDetails(
                    username,
                    "", // password not needed for already-authenticated token
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)),
                    userId,
                    businessId);

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.debug("Authenticated user '{}' (bid={}) on {}", username, businessId, request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }
}
