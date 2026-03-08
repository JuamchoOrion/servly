package co.edu.uniquindio.servly.DTO.Auth;

import co.edu.uniquindio.servly.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta exitosa de autenticación.
 * Se retorna al completar el login (sin 2FA) o al verificar el código 2FA.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private String userId;
    private String name;
    private String email;
    
    /**
     * Rol único del usuario (para compatibilidad)
     */
    private Role role;
    
    /**
     * Lista de roles del usuario (formato estándar para frontend)
     */
    @Builder.Default
    private List<String> roles = List.of();

    /**
     * Indica si el usuario debe cambiar su contraseña antes de continuar.
     * Si es true, el frontend debe redirigir a la vista de cambio de contraseña.
     */
    @Builder.Default
    private boolean mustChangePassword = false;

    /**
     * Indica si el usuario ya completó su primer login.
     */
    @Builder.Default
    private boolean firstLoginCompleted = false;
}
