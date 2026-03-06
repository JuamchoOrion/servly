package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.*;
import co.edu.uniquindio.servly.DTO.Auth.RegisterRequest;
import co.edu.uniquindio.servly.DTO.Roles.CreateEmployeeRequest;
import co.edu.uniquindio.servly.DTO.Roles.UpdateRoleRequest;
import co.edu.uniquindio.servly.DTO.Roles.UserResponse;
import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.model.entity.User;
import co.edu.uniquindio.servly.model.enums.AuthProvider;
import co.edu.uniquindio.servly.model.enums.Role;
import co.edu.uniquindio.servly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

/**
 * Operaciones de gestión de usuarios disponibles solo para el ADMIN:
 *  - Crear usuario con rol específico
 *  - Listar usuarios
 *  - Obtener usuario por ID
 *  - Cambiar rol
 *  - Activar / desactivar cuenta
 *  - Eliminar usuario
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService    emailService;

    public UserResponse createUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Ya existe una cuenta registrada con ese email");
        }

        // Solo otro ADMIN puede crear un ADMIN
        if (request.getRole() == Role.ADMIN) {
            String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            User current = userRepository.findByEmail(currentEmail)
                    .orElseThrow(() -> new AuthException("Usuario autenticado no encontrado"));
            if (current.getRole() != Role.ADMIN) {
                throw new AuthException("Solo un ADMIN puede crear otro ADMIN");
            }
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(request.getRole())
                .provider(AuthProvider.LOCAL)
                .twoFactorEnabled(request.isTwoFactorEnabled())
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        
        // Enviar email de bienvenida con credenciales
        emailService.sendWelcomeEmailWithTempCredentials(
                saved.getEmail(), saved.getName(),
                request.getPassword(), saved.getRole().name(),
                false,  // mustChangePassword = false (admin puede mantener su password)
                request.isTwoFactorEnabled()
        );

        log.info("ADMIN creó usuario: {} con rol: {}", saved.getEmail(), saved.getRole());
        return UserResponse.from(saved);
    }

    /**
     * Crea un empleado con contraseña temporal.
     * El empleado deberá cambiar la contraseña en su primer login.
     * 
     * Solo disponible para ADMIN.
     */
    public UserResponse createEmployee(CreateEmployeeRequest request) {
        // Validar que solo ADMIN pueda crear empleados
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User current = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new AuthException("Usuario autenticado no encontrado"));
        
        if (current.getRole() != Role.ADMIN) {
            throw new AuthException("Solo un ADMIN puede crear empleados");
        }

        // Validar email no exista
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Ya existe una cuenta registrada con ese email");
        }

        // Validar rol válido para empleado (no puede ser ADMIN)
        if (request.getRole() == Role.ADMIN) {
            throw new AuthException(
                "Para crear un ADMIN use el método createUser. " +
                "Este método es solo para roles de empleado: CASHIER, WAITER, KITCHEN, STOREKEEPER"
            );
        }

        // Generar contraseña temporal segura
        String tempPassword = generateSecureTempPassword();

        // Crear empleado con estado de primer login
        User employee = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(tempPassword))
                .name(request.getName())
                .lastName(request.getLastName())
                .address(request.getAddress())
                .role(request.getRole())
                .provider(AuthProvider.LOCAL)
                .mustChangePassword(true)           // ← OBLIGATORIO cambiar password
                .firstLoginCompleted(false)         // ← Primer login pendiente
                .twoFactorEnabled(true)             // ← 2FA obligatorio para empleados
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        User saved = userRepository.save(employee);
        emailService.sendWelcomeEmailWithTempCredentials(
                saved.getEmail(), saved.getName(),
                tempPassword, saved.getRole().name(),
                true,   // mustChangePassword = true (empleados deben cambiar password)
                true    // twoFactorEnabled = true (obligatorio para empleados)
        );

        log.info(
            "ADMIN creó empleado: {} con rol: {}. Contraseña temporal enviada.",
            saved.getEmail(), saved.getRole()
        );

        return UserResponse.from(saved);
    }

    /**
     * Genera una contraseña temporal segura de 12 caracteres.
     * Incluye mayúsculas, minúsculas, números y símbolos.
     */
    private String generateSecureTempPassword() {
        final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final String LOWER = "abcdefghijklmnopqrstuvwxyz";
        final String DIGITS = "0123456789";
        final String SPECIAL = "!@#$%^&*";
        
        final String ALL = UPPER + LOWER + DIGITS + SPECIAL;
        final int LENGTH = 12;
        
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(LENGTH);
        
        // Asegurar al menos un carácter de cada tipo
        password.append(UPPER.charAt(random.nextInt(UPPER.length())));
        password.append(LOWER.charAt(random.nextInt(LOWER.length())));
        password.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        password.append(SPECIAL.charAt(random.nextInt(SPECIAL.length())));
        
        // Completar con caracteres aleatorios
        for (int i = 4; i < LENGTH; i++) {
            password.append(ALL.charAt(random.nextInt(ALL.length())));
        }
        
        // Mezclar la contraseña
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            char temp = passwordArray[index];
            passwordArray[index] = passwordArray[i];
            passwordArray[i] = temp;
        }
        
        return new String(passwordArray);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(String userId) {
        return UserResponse.from(findOrThrow(userId));
    }

    public UserResponse updateUserRole(String userId, UpdateRoleRequest request) {
        User user = findOrThrow(userId);

        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getEmail().equals(currentEmail) && request.getRole() != Role.ADMIN) {
            throw new AuthException("No puedes cambiar tu propio rol de ADMIN");
        }

        user.setRole(request.getRole());
        log.info("Rol de {} actualizado a {}", user.getEmail(), request.getRole());
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse toggleUserStatus(String userId) {
        User user = findOrThrow(userId);

        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getEmail().equals(currentEmail)) {
            throw new AuthException("No puedes desactivar tu propia cuenta");
        }

        user.setEnabled(!user.isEnabled());
        log.info("Cuenta {} {}", user.getEmail(), user.isEnabled() ? "activada" : "desactivada");
        return UserResponse.from(userRepository.save(user));
    }

    public MessageResponse deleteUser(String userId) {
        User user = findOrThrow(userId);

        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getEmail().equals(currentEmail)) {
            throw new AuthException("No puedes eliminar tu propia cuenta");
        }

        userRepository.delete(user);
        log.info("ADMIN eliminó usuario: {}", user.getEmail());
        return new MessageResponse("Usuario " + user.getEmail() + " eliminado correctamente");
    }

    private User findOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Usuario no encontrado con ID: " + userId));
    }
}