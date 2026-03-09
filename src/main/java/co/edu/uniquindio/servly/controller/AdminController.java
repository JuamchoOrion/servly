package co.edu.uniquindio.servly.controller;

import co.edu.uniquindio.servly.DTO.Roles.CreateEmployeeRequest;
import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.DTO.Roles.UpdateRoleRequest;
import co.edu.uniquindio.servly.DTO.Roles.UserResponse;
import co.edu.uniquindio.servly.model.dto.metrics.AuthenticationMetricsDTO;
import co.edu.uniquindio.servly.service.UserService;
import co.edu.uniquindio.servly.service.AuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Endpoints exclusivos para ADMIN.
 * 
 * - Crear empleado con contraseña temporal
 * - Listar todos los usuarios
 * - Obtener usuario por ID
 * - Cambiar rol de usuario
 * - Activar/desactivar cuenta
 * - Eliminar usuario
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final AuditService auditService;

    /**
     * Crea un empleado con contraseña temporal.
     * El empleado recibirá un email con:
     *  - Contraseña temporal
     *  - Instrucciones de primer login
     *  - 2FA obligatorio
     * 
     * El empleado deberá cambiar la contraseña en su primer login.
     */
    @PostMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request) {
        
        UserResponse employee = userService.createEmployee(request);
        return ResponseEntity.ok(employee);
    }

    /**
     * Lista todos los usuarios del sistema.
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Obtiene un usuario por su ID.
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    /**
     * Cambia el rol de un usuario.
     */
    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable String userId,
            @Valid @RequestBody UpdateRoleRequest request) {
        
        return ResponseEntity.ok(userService.updateUserRole(userId, request));
    }

    /**
     * Activa o desactiva la cuenta de un usuario.
     */
    @PatchMapping("/users/{userId}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> toggleUserStatus(@PathVariable String userId) {
        return ResponseEntity.ok(userService.toggleUserStatus(userId));
    }

    /**
     * Elimina un usuario del sistema.
     */
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable String userId) {
        return ResponseEntity.ok(userService.deleteUser(userId));
    }

    /**
     * Obtiene las métricas de autenticación de los últimos 7 días.
     *
     * Métricas incluidas:
     *  - Tiempo promedio de autenticación (meta: < 2 segundos)
     *  - Tasa de accesos exitosos por rol (meta: > 95%)
     *  - Tiempo promedio de recuperación de contraseña (meta: < 5 minutos)
     *  - Tiempo de verificación en dos pasos (meta: < 60 segundos)
     *  - Tasa de expiración de códigos de verificación (meta: < 10%)
     *  - Duración promedio de sesión activa
     */
    @GetMapping("/metrics/auth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthenticationMetricsDTO> getAuthMetricsLast7Days() {
        return ResponseEntity.ok(auditService.getLast7DaysMetrics());
    }

    /**
     * Obtiene las métricas de autenticación de los últimos 30 días.
     */
    @GetMapping("/metrics/auth/30days")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthenticationMetricsDTO> getAuthMetricsLast30Days() {
        return ResponseEntity.ok(auditService.getLast30DaysMetrics());
    }

    /**
     * Obtiene las métricas de autenticación para un período personalizado.
     *
     * @param start Fecha de inicio (formato: yyyy-MM-dd'T'HH:mm:ss)
     * @param end Fecha de fin (formato: yyyy-MM-dd'T'HH:mm:ss)
     */
    @GetMapping("/metrics/auth/custom")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthenticationMetricsDTO> getAuthMetricsCustom(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return ResponseEntity.ok(auditService.getAuthenticationMetrics(start, end));
    }

    /**
     * Endpoint de health check para monitoreo.
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> healthCheck() {
        return ResponseEntity.ok(new MessageResponse("Service is healthy"));
    }
}
