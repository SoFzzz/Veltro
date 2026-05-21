package com.veltro.inventory.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VeltroUserDetailsTest {

    @Test
    @DisplayName("constructor — asigna todos los campos correctamente")
    void constructor_setsAllFields() {
        VeltroUserDetails details = new VeltroUserDetails(
                "admin", "hash123",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                10L, 100L);

        assertThat(details.getUsername()).isEqualTo("admin");
        assertThat(details.getPassword()).isEqualTo("hash123");
        assertThat(details.getUserId()).isEqualTo(10L);
        assertThat(details.getBusinessId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("getUserId — devuelve valor correcto")
    void getUserId_returnsCorrectValue() {
        VeltroUserDetails details = new VeltroUserDetails(
                "user", "pass", List.of(), 42L, 1L);
        assertThat(details.getUserId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("getBusinessId — devuelve valor correcto")
    void getBusinessId_returnsCorrectValue() {
        VeltroUserDetails details = new VeltroUserDetails(
                "user", "pass", List.of(), 1L, 99L);
        assertThat(details.getBusinessId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("getAuthorities — devuelve las authorities asignadas")
    void getAuthorities_returnsCorrectAuthorities() {
        var authorities = List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_WAREHOUSE"));
        VeltroUserDetails details = new VeltroUserDetails(
                "admin", "pass", authorities, 1L, 1L);

        assertThat(details.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_WAREHOUSE");
    }

    @Test
    @DisplayName("hereda funcionalidad de Spring User — isEnabled/isAccountNonLocked")
    void inheritsSpringUserFunctionality() {
        VeltroUserDetails details = new VeltroUserDetails(
                "user", "pass", List.of(), 1L, 1L);

        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("authorities vacías — devuelve colección vacía")
    void emptyAuthorities() {
        VeltroUserDetails details = new VeltroUserDetails(
                "user", "pass", List.of(), 1L, 1L);
        assertThat(details.getAuthorities()).isEmpty();
    }
}
