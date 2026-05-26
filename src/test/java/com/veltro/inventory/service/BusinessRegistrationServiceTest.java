package com.veltro.inventory.service;

import com.veltro.inventory.dto.auth.RegisterRequest;
import com.veltro.inventory.model.BusinessEntity;
import com.veltro.inventory.model.Role;
import com.veltro.inventory.model.UserEntity;
import com.veltro.inventory.repository.BusinessRepository;
import com.veltro.inventory.repository.UserRepository;
import com.veltro.inventory.util.PasswordHashUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessRegistrationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BusinessRepository businessRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private BusinessRegistrationService service;

    private RegisterRequest validRequest() {
        return new RegisterRequest("admin", "admin@test.com", PasswordHashUtils.sha256Hex("Password1"), "ADMIN", "Mi Negocio");
    }

    @Test
    @DisplayName("register — éxito crea negocio y usuario admin")
    void register_success() {
        RegisterRequest request = validRequest();
        BusinessEntity savedBusiness = new BusinessEntity();
        savedBusiness.setId(1L);
        savedBusiness.setName("Mi Negocio");

        UserEntity savedUser = new UserEntity();
        savedUser.setId(10L);
        savedUser.setUsername("admin");

        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);
        when(passwordEncoder.encode(PasswordHashUtils.sha256Hex("Password1"))).thenReturn("hashedPass");
        when(businessRepository.save(any(BusinessEntity.class))).thenReturn(savedBusiness);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        service.register(request);

        verify(businessRepository, times(2)).save(any(BusinessEntity.class));
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("register — nombre de negocio null lanza IllegalArgumentException")
    void register_nullBusinessName_throws() {
        RegisterRequest request = new RegisterRequest("admin", "admin@test.com", PasswordHashUtils.sha256Hex("Password1"), "ADMIN", null);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Business name is required");
    }

    @Test
    @DisplayName("register — nombre de negocio vacío lanza IllegalArgumentException")
    void register_blankBusinessName_throws() {
        RegisterRequest request = new RegisterRequest("admin", "admin@test.com", PasswordHashUtils.sha256Hex("Password1"), "ADMIN", "   ");

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Business name is required");
    }

    @Test
    @DisplayName("register — username duplicado lanza IllegalArgumentException")
    void register_duplicateUsername_throws() {
        RegisterRequest request = validRequest();
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already in use");

        verify(businessRepository, never()).save(any());
    }

    @Test
    @DisplayName("register — email duplicado lanza IllegalArgumentException")
    void register_duplicateEmail_throws() {
        RegisterRequest request = validRequest();
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already in use");

        verify(businessRepository, never()).save(any());
    }

    @Test
    @DisplayName("register — asigna rol ADMIN al usuario")
    void register_setsAdminRole() {
        RegisterRequest request = validRequest();
        BusinessEntity savedBusiness = new BusinessEntity();
        savedBusiness.setId(1L);

        UserEntity savedUser = new UserEntity();
        savedUser.setId(10L);

        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");
        when(businessRepository.save(any())).thenReturn(savedBusiness);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        service.register(request);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("register — hace trim al nombre del negocio")
    void register_trimsBusinessName() {
        RegisterRequest request = new RegisterRequest("admin", "admin@test.com", PasswordHashUtils.sha256Hex("Password1"), "ADMIN", "  Mi Negocio  ");
        BusinessEntity savedBusiness = new BusinessEntity();
        savedBusiness.setId(1L);
        UserEntity savedUser = new UserEntity();
        savedUser.setId(10L);

        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");
        when(businessRepository.save(any(BusinessEntity.class))).thenReturn(savedBusiness);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        service.register(request);

        ArgumentCaptor<BusinessEntity> captor = ArgumentCaptor.forClass(BusinessEntity.class);
        verify(businessRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getName()).isEqualTo("Mi Negocio");
    }

    @Test
    @DisplayName("register — encripta la contraseña antes de guardar")
    void register_encodesPassword() {
        RegisterRequest request = validRequest();
        BusinessEntity savedBusiness = new BusinessEntity();
        savedBusiness.setId(1L);
        UserEntity savedUser = new UserEntity();
        savedUser.setId(10L);

        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);
        when(passwordEncoder.encode(PasswordHashUtils.sha256Hex("Password1"))).thenReturn("bcrypt-hash");
        when(businessRepository.save(any())).thenReturn(savedBusiness);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        service.register(request);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
    }
}
