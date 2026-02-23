package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.RegisterRequest;
import co.edu.uniquindio.servly.DTO.UpdateRoleRequest;
import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.DTO.UserResponse;
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
        emailService.sendWelcomeEmail(
                saved.getEmail(), saved.getName(),
                request.getPassword(), saved.getRole().name());

        log.info("ADMIN creó usuario: {} con rol: {}", saved.getEmail(), saved.getRole());
        return UserResponse.from(saved);
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