package com.veltro.inventory.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clearOverride();
    }

    private void authenticateAs(Long userId, Long businessId, String username) {
        VeltroUserDetails principal = new VeltroUserDetails(
                username, "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                userId, businessId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, "password", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("getBusinessId")
    class GetBusinessId {

        @Test
        @DisplayName("desde SecurityContext devuelve businessId correcto")
        void fromSecurityContext() {
            authenticateAs(10L, 100L, "admin");
            assertThat(TenantContext.getBusinessId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("desde override devuelve override")
        void fromOverride() {
            TenantContext.setOverride(200L, 20L, "async-user");
            assertThat(TenantContext.getBusinessId()).isEqualTo(200L);
        }

        @Test
        @DisplayName("override tiene prioridad sobre SecurityContext")
        void overrideTakesPrecedence() {
            authenticateAs(10L, 100L, "admin");
            TenantContext.setOverride(200L, 20L, "override");
            assertThat(TenantContext.getBusinessId()).isEqualTo(200L);
        }
    }

    @Nested
    @DisplayName("getUserId")
    class GetUserId {

        @Test
        @DisplayName("desde SecurityContext devuelve userId correcto")
        void fromSecurityContext() {
            authenticateAs(10L, 100L, "admin");
            assertThat(TenantContext.getUserId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("desde override devuelve override")
        void fromOverride() {
            TenantContext.setOverride(200L, 20L, "async-user");
            assertThat(TenantContext.getUserId()).isEqualTo(20L);
        }
    }

    @Nested
    @DisplayName("getUsername")
    class GetUsername {

        @Test
        @DisplayName("desde SecurityContext devuelve username correcto")
        void fromSecurityContext() {
            authenticateAs(10L, 100L, "admin");
            assertThat(TenantContext.getUsername()).isEqualTo("admin");
        }

        @Test
        @DisplayName("desde override devuelve override")
        void fromOverride() {
            TenantContext.setOverride(200L, 20L, "async-user");
            assertThat(TenantContext.getUsername()).isEqualTo("async-user");
        }
    }

    @Nested
    @DisplayName("getOptionalUsername")
    class GetOptionalUsername {

        @Test
        @DisplayName("usuario autenticado devuelve Optional con username")
        void authenticated() {
            authenticateAs(10L, 100L, "admin");
            Optional<String> result = TenantContext.getOptionalUsername();
            assertThat(result).isPresent().contains("admin");
        }

        @Test
        @DisplayName("sin autenticación devuelve Optional vacío")
        void anonymous_returnsEmpty() {
            Optional<String> result = TenantContext.getOptionalUsername();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("con override devuelve override")
        void fromOverride() {
            TenantContext.setOverride(200L, 20L, "async-user");
            Optional<String> result = TenantContext.getOptionalUsername();
            assertThat(result).isPresent().contains("async-user");
        }
    }

    @Nested
    @DisplayName("clearOverride")
    class ClearOverride {

        @Test
        @DisplayName("limpia todos los valores del override")
        void clearsAllValues() {
            TenantContext.setOverride(200L, 20L, "async-user");
            TenantContext.clearOverride();

            // Sin override ni SecurityContext, debería fallar
            assertThatThrownBy(TenantContext::getBusinessId)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    @DisplayName("sin autenticación ni override — lanza IllegalStateException")
    void noAuth_throwsIllegalState() {
        assertThatThrownBy(TenantContext::getBusinessId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No authenticated VeltroUserDetails");
    }

    @Test
    @DisplayName("setOverride y clearOverride — ciclo completo")
    void setOverride_clearOverride_fullCycle() {
        TenantContext.setOverride(300L, 30L, "batch-user");

        assertThat(TenantContext.getBusinessId()).isEqualTo(300L);
        assertThat(TenantContext.getUserId()).isEqualTo(30L);
        assertThat(TenantContext.getUsername()).isEqualTo("batch-user");

        TenantContext.clearOverride();

        assertThatThrownBy(TenantContext::getBusinessId)
                .isInstanceOf(IllegalStateException.class);
    }
}
