package com.veltro.inventory.config;

import com.veltro.inventory.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration (B1-02, B1-03).
 *
 * Security rules:
 * <ul>
 *   <li>Public: {@code POST /api/v1/auth/**}, {@code GET /actuator/health}.</li>
 *   <li>Catalog write endpoints ({@code POST/PUT /api/v1/products/**},
 *       {@code POST/PUT /api/v1/categories/**}): ADMIN or WAREHOUSE role —
 *       enforced via {@code @PreAuthorize} on each controller method.</li>
 *   <li>Audit endpoints: ADMIN only.</li>
 *   <li>All other endpoints: any authenticated user.</li>
 * </ul>
 *
 * Session policy: STATELESS (JWT bearer tokens only — no HTTP session).
 * CSRF: disabled for stateless REST API.
 * BCrypt cost factor: 12 (spec requirement).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final LoginRateLimitFilter loginRateLimitFilter;
    private final UserDetailsService userDetailsService;

    @Value("${cors.allowed-origins:http://localhost:5173,https://localhost:5173,http://127.0.0.1:5173,https://127.0.0.1:5173,http://localhost:5174,https://localhost:5174,http://127.0.0.1:5174,https://127.0.0.1:5174}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/v1/alerts/debug-all").permitAll()
                .requestMatchers("/api/v1/alerts/debug-inventory").permitAll()
                .requestMatchers("/api/v1/alerts/debug-evaluate-all").permitAll()
                .requestMatchers("/api/v1/alerts/debug-configurations").permitAll()
                // Role-restricted endpoints (more will be added in B1-03, B2-x)
                .requestMatchers("/api/v1/audit/**").hasRole("ADMIN")
                // Everything else requires a valid token
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS configuration driven by {@code cors.allowed-origins} property.
     * Defaults to localhost:5173 for local development.
     * In production, set {@code CORS_ALLOWED_ORIGINS} env var to the Vercel domain.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = new java.util.ArrayList<>(Arrays.asList(allowedOrigins.split(",")));
        if (!origins.contains("https://veltro-frontend-a80052c6811a.herokuapp.com")) {
            origins.add("https://veltro-frontend-a80052c6811a.herokuapp.com");
        }
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * BCrypt with cost factor 12 (spec requirement, B1-02 backlog task).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
