package co.edu.uniquindio.servly.DTO;

import co.edu.uniquindio.servly.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Role   role;
}
