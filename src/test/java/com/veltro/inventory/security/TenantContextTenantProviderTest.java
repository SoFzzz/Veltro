package com.veltro.inventory.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTenantProviderTest {

    private final TenantContextTenantProvider provider = new TenantContextTenantProvider();

    @BeforeEach
    void setUp() {
        VeltroUserDetails principal = new VeltroUserDetails(
                "test-user", "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                10L, 100L);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, "password", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clearOverride();
    }

    @Test
    @DisplayName("getBusinessId — delega a TenantContext")
    void getBusinessId_delegatesToTenantContext() {
        assertThat(provider.getBusinessId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("getUserId — delega a TenantContext")
    void getUserId_delegatesToTenantContext() {
        assertThat(provider.getUserId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getUsername — delega a TenantContext")
    void getUsername_delegatesToTenantContext() {
        assertThat(provider.getUsername()).isEqualTo("test-user");
    }

    @Test
    @DisplayName("getOptionalUsername — devuelve Optional con username")
    void getOptionalUsername_returnsPresent() {
        Optional<String> result = provider.getOptionalUsername();
        assertThat(result).isPresent().contains("test-user");
    }

    @Test
    @DisplayName("getOptionalUsername — sin auth devuelve vacío")
    void getOptionalUsername_noAuth_returnsEmpty() {
        SecurityContextHolder.clearContext();
        Optional<String> result = provider.getOptionalUsername();
        assertThat(result).isEmpty();
    }
}
