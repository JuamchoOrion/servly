package co.edu.uniquindio.servly.controller;

import co.edu.uniquindio.servly.DTO.CreateEmployeeRequest;
import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.DTO.UpdateRoleRequest;
import co.edu.uniquindio.servly.DTO.UserResponse;
import co.edu.uniquindio.servly.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}
