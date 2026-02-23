package co.edu.uniquindio.servly.controller;

import co.edu.uniquindio.servly.DTO.RegisterRequest;
import co.edu.uniquindio.servly.DTO.UpdateRoleRequest;
import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.DTO.UserResponse;
import co.edu.uniquindio.servly.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de gestión de usuarios — solo ADMIN.
 *
 * POST   /api/admin/users              → Crear usuario
 * GET    /api/admin/users              → Listar todos
 * GET    /api/admin/users/{id}         → Ver usuario por ID
 * PATCH  /api/admin/users/{id}/role    → Cambiar rol
 * PATCH  /api/admin/users/{id}/toggle  → Activar / desactivar
 * DELETE /api/admin/users/{id}         → Eliminar
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody RegisterRequest request) {
        return userService.createUser(request);
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable String id) {
        return userService.getUserById(id);
    }

    @PatchMapping("/{id}/role")
    public UserResponse updateUserRole(
            @PathVariable String id,
            @Valid @RequestBody UpdateRoleRequest request) {
        return userService.updateUserRole(id, request);
    }

    @PatchMapping("/{id}/toggle")
    public UserResponse toggleUserStatus(@PathVariable String id) {
        return userService.toggleUserStatus(id);
    }

    @DeleteMapping("/{id}")
    public MessageResponse deleteUser(@PathVariable String id) {
        return userService.deleteUser(id);
    }
}