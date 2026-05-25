package com.veltro.inventory.service;

import com.veltro.inventory.config.JwtProperties;
import com.veltro.inventory.dto.auth.ChangePasswordRequest;
import com.veltro.inventory.dto.auth.LoginRequest;
import com.veltro.inventory.dto.auth.LoginResponse;
import com.veltro.inventory.dto.auth.RefreshRequest;
import com.veltro.inventory.exception.NotFoundException;
import com.veltro.inventory.model.Role;
import com.veltro.inventory.model.BusinessEntity;
import com.veltro.inventory.model.UserEntity;
import com.veltro.inventory.repository.BusinessRepository;
import com.veltro.inventory.repository.UserRepository;
import com.veltro.inventory.security.CustomUserDetailsService;
import com.veltro.inventory.security.JwtTokenProvider;
import com.veltro.inventory.security.VeltroUserDetails;
import com.veltro.inventory.util.PasswordHashUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private UserRepository userRepository;
    @Mock private BusinessRepository businessRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtProperties jwtProperties;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    @DisplayName("login — éxito con VeltroUserDetails devuelve businessId")
    void login_success_withVeltroUserDetails() {
        LoginRequest request = new LoginRequest("admin", PasswordHashUtils.sha256Hex("password123"));
        VeltroUserDetails details = new VeltroUserDetails(
                "admin", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                10L, 100L);

        when(userDetailsService.loadUserByUsername("admin")).thenReturn(details);
        when(jwtTokenProvider.generateAccessToken(details)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(details)).thenReturn("refresh-token");
        when(jwtProperties.accessTokenExpiration()).thenReturn(3600L);

        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("admin@test.com");
        when(userRepository.findByUsernameAndActiveTrue("admin")).thenReturn(Optional.of(userEntity));

        BusinessEntity business = new BusinessEntity();
        business.setName("Test Business");
        when(businessRepository.findById(100L)).thenReturn(Optional.of(business));

        LoginResponse response = authenticationService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.username()).isEqualTo("admin");
        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.businessId()).isEqualTo(100L);
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.email()).isEqualTo("admin@test.com");
        assertThat(response.businessName()).isEqualTo("Test Business");
        assertThat(response.adminName()).isNull();
    }

    @Test
    @DisplayName("login — éxito con UserDetails estándar devuelve businessId null")
    void login_success_withRegularUserDetails() {
        LoginRequest request = new LoginRequest("user", PasswordHashUtils.sha256Hex("password123"));
        UserDetails details = new User("user", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_CASHIER")));

        when(userDetailsService.loadUserByUsername("user")).thenReturn(details);
        when(jwtTokenProvider.generateAccessToken(details)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(details)).thenReturn("refresh-token");
        when(jwtProperties.accessTokenExpiration()).thenReturn(3600L);

        LoginResponse response = authenticationService.login(request);

        assertThat(response.businessId()).isNull();
        assertThat(response.role()).isEqualTo("CASHIER");
    }

    @Test
    @DisplayName("login — credenciales inválidas propaga excepción")
    void login_authenticationFails_propagatesException() {
        LoginRequest request = new LoginRequest("admin", PasswordHashUtils.sha256Hex("wrong"));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login — sin authorities devuelve role vacío")
    void login_noAuthorities_returnsEmptyRole() {
        LoginRequest request = new LoginRequest("user", PasswordHashUtils.sha256Hex("pass123456"));
        UserDetails details = new User("user", "hash", List.of());

        when(userDetailsService.loadUserByUsername("user")).thenReturn(details);
        when(jwtTokenProvider.generateAccessToken(details)).thenReturn("at");
        when(jwtTokenProvider.generateRefreshToken(details)).thenReturn("rt");
        when(jwtProperties.accessTokenExpiration()).thenReturn(900L);

        LoginResponse response = authenticationService.login(request);
        assertThat(response.role()).isEmpty();
    }

    @Test
    @DisplayName("refresh — éxito devuelve nuevo access token")
    void refresh_success() {
        RefreshRequest request = new RefreshRequest("valid-refresh");
        VeltroUserDetails details = new VeltroUserDetails(
                "admin", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                10L, 100L);

        when(jwtTokenProvider.isValidRefreshToken("valid-refresh")).thenReturn(true);
        when(jwtTokenProvider.extractUsername("valid-refresh")).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(details);
        when(jwtTokenProvider.generateAccessToken(details)).thenReturn("new-access-token");
        when(jwtProperties.accessTokenExpiration()).thenReturn(3600L);

        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("admin@test.com");
        when(userRepository.findByUsernameAndActiveTrue("admin")).thenReturn(Optional.of(userEntity));

        BusinessEntity business = new BusinessEntity();
        business.setName("Test Business");
        when(businessRepository.findById(100L)).thenReturn(Optional.of(business));

        LoginResponse response = authenticationService.refresh(request);

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("valid-refresh");
        assertThat(response.businessId()).isEqualTo(100L);
        assertThat(response.email()).isEqualTo("admin@test.com");
        assertThat(response.businessName()).isEqualTo("Test Business");
        assertThat(response.adminName()).isNull();
    }

    @Test
    @DisplayName("refresh — token inválido lanza IllegalArgumentException")
    void refresh_invalidToken_throwsIllegalArgument() {
        RefreshRequest request = new RefreshRequest("invalid-token");
        when(jwtTokenProvider.isValidRefreshToken("invalid-token")).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.refresh(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid or expired");
    }

    @Test
    @DisplayName("logout — no lanza excepción (stateless)")
    void logout_doesNotThrow() {
        assertThatCode(() -> authenticationService.logout("admin"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("changePassword — éxito actualiza contraseña")
    void changePassword_success() {
        ChangePasswordRequest request = new ChangePasswordRequest(PasswordHashUtils.sha256Hex("oldPass"), PasswordHashUtils.sha256Hex("newPass123"));
        UserEntity user = new UserEntity();
        user.setUsername("admin");
        user.setPasswordHash("encoded-old");

        when(userRepository.findByUsernameAndActiveTrue("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PasswordHashUtils.sha256Hex("oldPass"), "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode(PasswordHashUtils.sha256Hex("newPass123"))).thenReturn("encoded-new");

        authenticationService.changePassword("admin", request);

        assertThat(user.getPasswordHash()).isEqualTo("encoded-new");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("changePassword — usuario no encontrado lanza NotFoundException")
    void changePassword_userNotFound_throwsNotFoundException() {
        ChangePasswordRequest request = new ChangePasswordRequest(PasswordHashUtils.sha256Hex("old"), PasswordHashUtils.sha256Hex("newPass123"));
        when(userRepository.findByUsernameAndActiveTrue("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.changePassword("ghost", request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("ghost");
    }

    @Test
    @DisplayName("changePassword — contraseña actual incorrecta lanza IllegalArgumentException")
    void changePassword_wrongCurrentPassword_throwsIllegalArgument() {
        ChangePasswordRequest request = new ChangePasswordRequest(PasswordHashUtils.sha256Hex("wrong"), PasswordHashUtils.sha256Hex("newPass123"));
        UserEntity user = new UserEntity();
        user.setPasswordHash("encoded-old");

        when(userRepository.findByUsernameAndActiveTrue("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PasswordHashUtils.sha256Hex("wrong"), "encoded-old")).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.changePassword("admin", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("incorrect");
    }
}
