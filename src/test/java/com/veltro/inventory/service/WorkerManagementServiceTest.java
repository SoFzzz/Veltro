package com.veltro.inventory.service;

import com.veltro.inventory.dto.auth.RegisterRequest;
import com.veltro.inventory.dto.auth.WorkerCreatedResponse;
import com.veltro.inventory.dto.auth.WorkerResponse;
import com.veltro.inventory.exception.NotFoundException;
import com.veltro.inventory.model.Role;
import com.veltro.inventory.model.UserEntity;
import com.veltro.inventory.repository.UserRepository;
import com.veltro.inventory.util.PasswordHashUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkerManagementServiceTest {

    private static final Long BUSINESS_ID = 100L;

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private WorkerManagementService service;

    private UserEntity createWorkerEntity(Long id, String username, Role role, Long businessId) {
        UserEntity worker = new UserEntity();
        worker.setId(id);
        worker.setUsername(username);
        worker.setEmail(username + "@test.com");
        worker.setPasswordHash("hash");
        worker.setRole(role);
        worker.setBusinessId(businessId);
        worker.setActive(true);
        return worker;
    }

    @Nested
    @DisplayName("createWorker")
    class CreateWorker {

        @Test
        @DisplayName("éxito — crea cajero correctamente")
        void success_cashier() {
            RegisterRequest request = new RegisterRequest("cajero1", "cajero@test.com", PasswordHashUtils.sha256Hex("Password1"), "CASHIER", null);
            UserEntity saved = createWorkerEntity(1L, "cajero1", Role.CASHIER, BUSINESS_ID);
            saved.setCreatedAt(Instant.now());

            when(userRepository.findByUsernameAndBusinessId("cajero1", BUSINESS_ID)).thenReturn(Optional.empty());
            when(userRepository.findByEmailAndActiveTrue("cajero@test.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(PasswordHashUtils.sha256Hex("Password1"))).thenReturn("hash");
            when(userRepository.save(any(UserEntity.class))).thenReturn(saved);

            WorkerCreatedResponse response = service.createWorker(BUSINESS_ID, request);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.username()).isEqualTo("cajero1");
            assertThat(response.role()).isEqualTo("CASHIER");
        }

        @Test
        @DisplayName("éxito — crea almacenero correctamente")
        void success_warehouse() {
            RegisterRequest request = new RegisterRequest("bodega1", "bodega@test.com", PasswordHashUtils.sha256Hex("Password1"), "WAREHOUSE", null);
            UserEntity saved = createWorkerEntity(2L, "bodega1", Role.WAREHOUSE, BUSINESS_ID);
            saved.setCreatedAt(Instant.now());

            when(userRepository.findByUsernameAndBusinessId("bodega1", BUSINESS_ID)).thenReturn(Optional.empty());
            when(userRepository.findByEmailAndActiveTrue("bodega@test.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(PasswordHashUtils.sha256Hex("Password1"))).thenReturn("hash");
            when(userRepository.save(any(UserEntity.class))).thenReturn(saved);

            WorkerCreatedResponse response = service.createWorker(BUSINESS_ID, request);
            assertThat(response.role()).isEqualTo("WAREHOUSE");
        }

        @Test
        @DisplayName("rol ADMIN lanza IllegalArgumentException")
        void adminRole_throws() {
            RegisterRequest request = new RegisterRequest("admin2", "admin2@test.com", PasswordHashUtils.sha256Hex("Password1"), "ADMIN", null);

            assertThatThrownBy(() -> service.createWorker(BUSINESS_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ADMIN");
        }

        @Test
        @DisplayName("username duplicado en el negocio lanza IllegalArgumentException")
        void duplicateUsername_throws() {
            RegisterRequest request = new RegisterRequest("cajero1", "cajero@test.com", PasswordHashUtils.sha256Hex("Password1"), "CASHIER", null);
            when(userRepository.findByUsernameAndBusinessId("cajero1", BUSINESS_ID))
                    .thenReturn(Optional.of(new UserEntity()));

            assertThatThrownBy(() -> service.createWorker(BUSINESS_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Username already exists");
        }

        @Test
        @DisplayName("email duplicado lanza IllegalArgumentException")
        void duplicateEmail_throws() {
            RegisterRequest request = new RegisterRequest("nuevo", "existing@test.com", PasswordHashUtils.sha256Hex("Password1"), "CASHIER", null);
            when(userRepository.findByUsernameAndBusinessId("nuevo", BUSINESS_ID)).thenReturn(Optional.empty());
            when(userRepository.findByEmailAndActiveTrue("existing@test.com"))
                    .thenReturn(Optional.of(new UserEntity()));

            assertThatThrownBy(() -> service.createWorker(BUSINESS_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email already in use");
        }
    }

    @Nested
    @DisplayName("getWorkers")
    class GetWorkers {

        @Test
        @DisplayName("filtra admins y devuelve solo trabajadores")
        void filtersOutAdmins() {
            UserEntity admin = createWorkerEntity(1L, "admin", Role.ADMIN, BUSINESS_ID);
            UserEntity cashier = createWorkerEntity(2L, "cajero", Role.CASHIER, BUSINESS_ID);
            UserEntity warehouse = createWorkerEntity(3L, "bodega", Role.WAREHOUSE, BUSINESS_ID);

            when(userRepository.findAllByBusinessIdAndActiveTrue(BUSINESS_ID))
                    .thenReturn(List.of(admin, cashier, warehouse));

            List<WorkerResponse> result = service.getWorkers(BUSINESS_ID);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(WorkerResponse::role)
                    .containsExactly("CASHIER", "WAREHOUSE");
        }

        @Test
        @DisplayName("devuelve lista vacía cuando no hay trabajadores")
        void emptyList() {
            when(userRepository.findAllByBusinessIdAndActiveTrue(BUSINESS_ID))
                    .thenReturn(List.of());

            List<WorkerResponse> result = service.getWorkers(BUSINESS_ID);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deactivateWorker")
    class DeactivateWorker {

        @Test
        @DisplayName("éxito — desactiva trabajador")
        void success() {
            UserEntity worker = createWorkerEntity(1L, "cajero", Role.CASHIER, BUSINESS_ID);
            when(userRepository.findById(1L)).thenReturn(Optional.of(worker));

            service.deactivateWorker(1L, BUSINESS_ID);

            assertThat(worker.isActive()).isFalse();
            verify(userRepository).save(worker);
        }

        @Test
        @DisplayName("trabajador no encontrado lanza NotFoundException")
        void notFound_throws() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deactivateWorker(999L, BUSINESS_ID))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("negocio diferente lanza IllegalArgumentException")
        void differentBusiness_throws() {
            UserEntity worker = createWorkerEntity(1L, "cajero", Role.CASHIER, 999L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(worker));

            assertThatThrownBy(() -> service.deactivateWorker(1L, BUSINESS_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        @DisplayName("usuario ADMIN no puede ser desactivado")
        void adminUser_throws() {
            UserEntity admin = createWorkerEntity(1L, "admin", Role.ADMIN, BUSINESS_ID);
            when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

            assertThatThrownBy(() -> service.deactivateWorker(1L, BUSINESS_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ADMIN");
        }
    }

    @Nested
    @DisplayName("updateWorkerRole")
    class UpdateWorkerRole {

        @Test
        @DisplayName("éxito — actualiza rol de CASHIER a WAREHOUSE")
        void success() {
            UserEntity worker = createWorkerEntity(1L, "cajero", Role.CASHIER, BUSINESS_ID);
            when(userRepository.findById(1L)).thenReturn(Optional.of(worker));
            when(userRepository.save(worker)).thenReturn(worker);

            WorkerResponse response = service.updateWorkerRole(1L, "WAREHOUSE", BUSINESS_ID);

            assertThat(response.role()).isEqualTo("WAREHOUSE");
            assertThat(worker.getRole()).isEqualTo(Role.WAREHOUSE);
        }

        @Test
        @DisplayName("asignar ADMIN lanza IllegalArgumentException")
        void toAdmin_throws() {
            assertThatThrownBy(() -> service.updateWorkerRole(1L, "ADMIN", BUSINESS_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("trabajador no encontrado lanza NotFoundException")
        void notFound_throws() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateWorkerRole(999L, "CASHIER", BUSINESS_ID))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("negocio diferente lanza IllegalArgumentException")
        void differentBusiness_throws() {
            UserEntity worker = createWorkerEntity(1L, "cajero", Role.CASHIER, 999L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(worker));

            assertThatThrownBy(() -> service.updateWorkerRole(1L, "WAREHOUSE", BUSINESS_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("cambiar rol de ADMIN lanza IllegalArgumentException")
        void adminUser_throws() {
            UserEntity admin = createWorkerEntity(1L, "admin", Role.ADMIN, BUSINESS_ID);
            when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

            assertThatThrownBy(() -> service.updateWorkerRole(1L, "CASHIER", BUSINESS_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ADMIN");
        }
    }

    @Nested
    @DisplayName("parseWorkerRole")
    class ParseWorkerRole {

        @ParameterizedTest
        @ValueSource(strings = {"CASHIER", "cashier", "Cashier", "CAJERO", "cajero"})
        @DisplayName("reconoce variantes de CASHIER")
        void cashierVariants(String input) {
            assertThat(service.parseWorkerRole(input)).isEqualTo(Role.CASHIER);
        }

        @ParameterizedTest
        @ValueSource(strings = {"WAREHOUSE", "warehouse", "ALMACEN", "almacen", "BODEGA", "bodega"})
        @DisplayName("reconoce variantes de WAREHOUSE")
        void warehouseVariants(String input) {
            assertThat(service.parseWorkerRole(input)).isEqualTo(Role.WAREHOUSE);
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMIN", "admin", "ADMINISTRADOR", "administrador"})
        @DisplayName("reconoce variantes de ADMIN")
        void adminVariants(String input) {
            assertThat(service.parseWorkerRole(input)).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("null lanza IllegalArgumentException")
        void nullRole_throws() {
            assertThatThrownBy(() -> service.parseWorkerRole(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("blank lanza IllegalArgumentException")
        void blankRole_throws() {
            assertThatThrownBy(() -> service.parseWorkerRole("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rol inválido lanza IllegalArgumentException")
        void invalidRole_throws() {
            assertThatThrownBy(() -> service.parseWorkerRole("MANAGER"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
