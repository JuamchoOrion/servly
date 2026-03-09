package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.DTO.Auth.RegisterRequest;
import co.edu.uniquindio.servly.DTO.Roles.CreateEmployeeRequest;
import co.edu.uniquindio.servly.DTO.Roles.UpdateRoleRequest;
import co.edu.uniquindio.servly.DTO.Roles.UserResponse;
import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.model.entity.User;
import co.edu.uniquindio.servly.model.enums.AuthProvider;
import co.edu.uniquindio.servly.model.enums.Role;
import co.edu.uniquindio.servly.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks
    private UserService userService;

    // ── fixtures ──────────────────────────────────────────────────────────────
    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id("admin-1")
                .email("admin@test.com")
                .name("Admin")
                .role(Role.ADMIN)
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .build();

        regularUser = User.builder()
                .id("user-1")
                .email("waiter@test.com")
                .name("Waiter")
                .role(Role.WAITER)
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .build();
    }

    /** Simula un usuario autenticado en el SecurityContext */
    private void authenticateAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of())
        );
    }

    // =========================================================================
    // createUser
    // =========================================================================
    @Nested
    @DisplayName("createUser()")
    class CreateUser {

        private RegisterRequest buildRequest(Role role) {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("nuevo@test.com");
            req.setPassword("pass123");
            req.setName("Nuevo");
            req.setRole(role);
            req.setTwoFactorEnabled(false);
            return req;
        }

        @Test
        @DisplayName("Debe crear usuario no-ADMIN sin verificar el rol del autenticado")
        void shouldCreateNonAdminUser() {
            RegisterRequest request = buildRequest(Role.WAITER);
            User saved = regularUser.toBuilder().id("new-1").email("nuevo@test.com").build();

            when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
            when(passwordEncoder.encode("pass123")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenReturn(saved);

            UserResponse result = userService.createUser(request);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("nuevo@test.com");
            verify(emailService).sendWelcomeEmailWithTempCredentials(
                    eq("nuevo@test.com"), anyString(), eq("pass123"),
                    anyString(), eq(false), eq(false));
        }

        @Test
        @DisplayName("Debe crear un ADMIN cuando el autenticado también es ADMIN")
        void shouldCreateAdminWhenAuthenticatedIsAdmin() {
            authenticateAs("admin@test.com");
            RegisterRequest request = buildRequest(Role.ADMIN);
            User saved = adminUser.toBuilder().id("admin-2").email("nuevo@test.com").build();

            when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
            when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
            when(passwordEncoder.encode("pass123")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenReturn(saved);

            UserResponse result = userService.createUser(request);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Debe lanzar AuthException si el email ya está registrado")
        void shouldThrowWhenEmailAlreadyExists() {
            when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(buildRequest(Role.WAITER)))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Ya existe una cuenta");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar AuthException si un no-ADMIN intenta crear un ADMIN")
        void shouldThrowWhenNonAdminTriesToCreateAdmin() {
            authenticateAs("waiter@test.com");
            RegisterRequest request = buildRequest(Role.ADMIN);

            when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
            when(userRepository.findByEmail("waiter@test.com")).thenReturn(Optional.of(regularUser));

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Solo un ADMIN puede crear otro ADMIN");

            verify(userRepository, never()).save(any());
        }
    }

    // =========================================================================
    // createEmployee
    // =========================================================================
    @Nested
    @DisplayName("createEmployee()")
    class CreateEmployee {

        private CreateEmployeeRequest buildEmployeeRequest(Role role) {
            CreateEmployeeRequest req = new CreateEmployeeRequest();
            req.setEmail("empleado@test.com");
            req.setName("Empleado");
            req.setLastName("Apellido");
            req.setAddress("Calle 1");
            req.setRole(role);
            return req;
        }

        @Test
        @DisplayName("Debe crear empleado con contraseña temporal y mustChangePassword=true")
        void shouldCreateEmployeeWithTempPassword() {
            authenticateAs("admin@test.com");
            CreateEmployeeRequest request = buildEmployeeRequest(Role.CASHIER);

            User savedEmployee = User.builder()
                    .id("emp-1").email("empleado@test.com").name("Empleado")
                    .role(Role.CASHIER).mustChangePassword(true)
                    .twoFactorEnabled(true).enabled(true).build();

            when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
            when(userRepository.existsByEmail("empleado@test.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded-temp");
            when(userRepository.save(any(User.class))).thenReturn(savedEmployee);

            UserResponse result = userService.createEmployee(request);

            assertThat(result).isNotNull();
            verify(emailService).sendWelcomeEmailWithTempCredentials(
                    eq("empleado@test.com"), anyString(), anyString(),
                    eq("CASHIER"), eq(true), eq(true));
        }

        @Test
        @DisplayName("Debe lanzar AuthException si el autenticado no es ADMIN")
        void shouldThrowWhenAuthenticatedIsNotAdmin() {
            authenticateAs("waiter@test.com");
            when(userRepository.findByEmail("waiter@test.com")).thenReturn(Optional.of(regularUser));

            assertThatThrownBy(() -> userService.createEmployee(buildEmployeeRequest(Role.CASHIER)))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Solo un ADMIN puede crear empleados");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar AuthException si el email del empleado ya existe")
        void shouldThrowWhenEmailAlreadyExists() {
            authenticateAs("admin@test.com");
            when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
            when(userRepository.existsByEmail("empleado@test.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createEmployee(buildEmployeeRequest(Role.WAITER)))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Ya existe una cuenta");
        }

        @Test
        @DisplayName("Debe lanzar AuthException si se intenta crear un empleado con rol ADMIN")
        void shouldThrowWhenRoleIsAdmin() {
            authenticateAs("admin@test.com");
            when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
            when(userRepository.existsByEmail("empleado@test.com")).thenReturn(false);

            assertThatThrownBy(() -> userService.createEmployee(buildEmployeeRequest(Role.ADMIN)))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Este método es solo para roles de empleado");
        }

        @Test
        @DisplayName("La contraseña temporal debe tener exactamente 12 caracteres")
        void shouldGenerateTempPasswordOf12Chars() {
            authenticateAs("admin@test.com");
            CreateEmployeeRequest request = buildEmployeeRequest(Role.KITCHEN);

            User savedEmployee = User.builder()
                    .id("emp-2").email("empleado@test.com").name("Empleado")
                    .role(Role.KITCHEN).mustChangePassword(true)
                    .twoFactorEnabled(true).enabled(true).build();

            when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
            when(userRepository.existsByEmail("empleado@test.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenAnswer(inv -> {
                String rawPassword = inv.getArgument(0);
                assertThat(rawPassword).hasSize(12);
                return "encoded";
            });
            when(userRepository.save(any(User.class))).thenReturn(savedEmployee);

            userService.createEmployee(request);
        }
    }

    // =========================================================================
    // getAllUsers
    // =========================================================================
    @Nested
    @DisplayName("getAllUsers()")
    class GetAllUsers {

        @Test
        @DisplayName("Debe retornar lista de UserResponse con todos los usuarios")
        void shouldReturnAllUsers() {
            when(userRepository.findAll()).thenReturn(List.of(adminUser, regularUser));

            List<UserResponse> result = userService.getAllUsers();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(UserResponse::getEmail)
                    .containsExactly("admin@test.com", "waiter@test.com");
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay usuarios")
        void shouldReturnEmptyList() {
            when(userRepository.findAll()).thenReturn(List.of());

            assertThat(userService.getAllUsers()).isEmpty();
        }
    }

    // =========================================================================
    // getUserById
    // =========================================================================
    @Nested
    @DisplayName("getUserById()")
    class GetUserById {

        @Test
        @DisplayName("Debe retornar UserResponse cuando el usuario existe")
        void shouldReturnUser() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(regularUser));

            UserResponse result = userService.getUserById("user-1");

            assertThat(result.getEmail()).isEqualTo("waiter@test.com");
        }

        @Test
        @DisplayName("Debe lanzar AuthException cuando el usuario no existe")
        void shouldThrowWhenNotFound() {
            when(userRepository.findById("x")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById("x"))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }
    }

    // =========================================================================
    // updateUserRole
    // =========================================================================
    @Nested
    @DisplayName("updateUserRole()")
    class UpdateUserRole {

        @Test
        @DisplayName("Debe actualizar el rol del usuario correctamente")
        void shouldUpdateRole() {
            authenticateAs("admin@test.com");
            UpdateRoleRequest request = new UpdateRoleRequest();
            request.setRole(Role.CASHIER);

            when(userRepository.findById("user-1")).thenReturn(Optional.of(regularUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserResponse result = userService.updateUserRole("user-1", request);

            assertThat(result.getRole()).isEqualTo(Role.CASHIER);
        }

        @Test
        @DisplayName("Debe lanzar AuthException si el ADMIN intenta quitarse su propio rol")
        void shouldThrowWhenAdminChangesOwnRole() {
            authenticateAs("admin@test.com");
            UpdateRoleRequest request = new UpdateRoleRequest();
            request.setRole(Role.WAITER); // intenta bajar su propio rol

            when(userRepository.findById("admin-1")).thenReturn(Optional.of(adminUser));

            assertThatThrownBy(() -> userService.updateUserRole("admin-1", request))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("No puedes cambiar tu propio rol de ADMIN");
        }

        @Test
        @DisplayName("Debe lanzar AuthException si el usuario no existe")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById("x")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUserRole("x", new UpdateRoleRequest()))
                    .isInstanceOf(AuthException.class);
        }
    }

    // =========================================================================
    // toggleUserStatus
    // =========================================================================
    @Nested
    @DisplayName("toggleUserStatus()")
    class ToggleUserStatus {

        @Test
        @DisplayName("Debe desactivar un usuario activo")
        void shouldDeactivateActiveUser() {
            authenticateAs("admin@test.com");
            regularUser.setEnabled(true);

            when(userRepository.findById("user-1")).thenReturn(Optional.of(regularUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserResponse result = userService.toggleUserStatus("user-1");

            assertThat(result.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Debe activar un usuario inactivo")
        void shouldActivateInactiveUser() {
            authenticateAs("admin@test.com");
            regularUser.setEnabled(false);

            when(userRepository.findById("user-1")).thenReturn(Optional.of(regularUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserResponse result = userService.toggleUserStatus("user-1");

            assertThat(result.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Debe lanzar AuthException si el ADMIN intenta desactivar su propia cuenta")
        void shouldThrowWhenAdminTogglesOwnAccount() {
            authenticateAs("admin@test.com");
            when(userRepository.findById("admin-1")).thenReturn(Optional.of(adminUser));

            assertThatThrownBy(() -> userService.toggleUserStatus("admin-1"))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("No puedes desactivar tu propia cuenta");
        }

        @Test
        @DisplayName("Debe lanzar AuthException si el usuario no existe")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById("x")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.toggleUserStatus("x"))
                    .isInstanceOf(AuthException.class);
        }
    }

    // =========================================================================
    // deleteUser
    // =========================================================================
    @Nested
    @DisplayName("deleteUser()")
    class DeleteUser {

        @Test
        @DisplayName("Debe eliminar el usuario y retornar mensaje de confirmación")
        void shouldDeleteUserSuccessfully() {
            authenticateAs("admin@test.com");
            when(userRepository.findById("user-1")).thenReturn(Optional.of(regularUser));

            MessageResponse result = userService.deleteUser("user-1");

            assertThat(result.getMessage()).contains("waiter@test.com");
            verify(userRepository).delete(regularUser);
        }

        @Test
        @DisplayName("Debe lanzar AuthException si el ADMIN intenta eliminarse a sí mismo")
        void shouldThrowWhenAdminDeletesOwnAccount() {
            authenticateAs("admin@test.com");
            when(userRepository.findById("admin-1")).thenReturn(Optional.of(adminUser));

            assertThatThrownBy(() -> userService.deleteUser("admin-1"))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("No puedes eliminar tu propia cuenta");

            verify(userRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Debe lanzar AuthException si el usuario no existe")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById("x")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser("x"))
                    .isInstanceOf(AuthException.class);

            verify(userRepository, never()).delete(any());
        }
    }
}