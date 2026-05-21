package com.veltro.inventory.security;

import com.veltro.inventory.model.Role;
import com.veltro.inventory.model.UserEntity;
import com.veltro.inventory.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private UserEntity createUser(Long id, String username, Role role, Long businessId) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setPasswordHash("bcrypt-hash");
        user.setRole(role);
        user.setBusinessId(businessId);
        user.setActive(true);
        return user;
    }

    @Test
    @DisplayName("loadUserByUsername — éxito devuelve VeltroUserDetails con datos correctos")
    void loadUserByUsername_success() {
        UserEntity user = createUser(10L, "admin", Role.ADMIN, 100L);
        when(userRepository.findByUsernameAndActiveTrue("admin")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("admin");

        assertThat(details).isInstanceOf(VeltroUserDetails.class);
        assertThat(details.getUsername()).isEqualTo("admin");
        assertThat(details.getPassword()).isEqualTo("bcrypt-hash");
    }

    @Test
    @DisplayName("loadUserByUsername — usuario no encontrado lanza UsernameNotFoundException")
    void loadUserByUsername_notFound_throws() {
        when(userRepository.findByUsernameAndActiveTrue("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost");
    }

    @Test
    @DisplayName("loadUserByUsername — devuelve authority ROLE_ADMIN para admin")
    void loadUserByUsername_returnsCorrectAuthority_admin() {
        UserEntity user = createUser(10L, "admin", Role.ADMIN, 100L);
        when(userRepository.findByUsernameAndActiveTrue("admin")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("admin");

        assertThat(details.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("loadUserByUsername — devuelve authority ROLE_CASHIER para cajero")
    void loadUserByUsername_returnsCorrectAuthority_cashier() {
        UserEntity user = createUser(20L, "cajero", Role.CASHIER, 100L);
        when(userRepository.findByUsernameAndActiveTrue("cajero")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("cajero");

        assertThat(details.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_CASHIER");
    }

    @Test
    @DisplayName("loadUserByUsername — devuelve businessId correcto en VeltroUserDetails")
    void loadUserByUsername_returnsCorrectBusinessId() {
        UserEntity user = createUser(10L, "admin", Role.ADMIN, 42L);
        when(userRepository.findByUsernameAndActiveTrue("admin")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("admin");

        VeltroUserDetails veltroDetails = (VeltroUserDetails) details;
        assertThat(veltroDetails.getBusinessId()).isEqualTo(42L);
        assertThat(veltroDetails.getUserId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("loadUserByUsername — devuelve authority ROLE_WAREHOUSE para almacenero")
    void loadUserByUsername_returnsCorrectAuthority_warehouse() {
        UserEntity user = createUser(30L, "bodega", Role.WAREHOUSE, 100L);
        when(userRepository.findByUsernameAndActiveTrue("bodega")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("bodega");

        assertThat(details.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_WAREHOUSE");
    }
}
