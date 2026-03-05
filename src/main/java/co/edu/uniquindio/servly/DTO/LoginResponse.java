package co.edu.uniquindio.servly.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta exitosa del endpoint de login.
 * Contiene el JWT y la información del usuario autenticado.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * JWT token para autenticación en requests posteriores.
     * Usar en header: Authorization: Bearer <token>
     * Expiración: 24 horas
     */
    private String token;

    /**
     * Token para renovar el access token cuando expire.
     * Expiración: 7 días
     * Se guarda en BD y puede ser revocado
     */
    private String refreshToken;

    /**
     * Email del usuario autenticado
     */
    private String email;

    /**
     * Array de roles del usuario
     * Ejemplo: ["STAFF", "ADMIN"]
     */
    private String[] roles;

    /**
     * Nombre completo del usuario
     */
    private String name;

    /**
     * Indica si el usuario debe cambiar contraseña en primer login
     */
    private boolean mustChangePassword;

    /**
     * Mensaje adicional al cliente (ej: "2FA requerido")
     */
    private String message;
}

