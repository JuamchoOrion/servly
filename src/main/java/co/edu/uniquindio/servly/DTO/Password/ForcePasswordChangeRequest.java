package co.edu.uniquindio.servly.DTO.Password;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para forzar el cambio de contraseña en el primer login.
 * Las validaciones de fortaleza de contraseña se realizan en el servicio
 * para devolver mensajes de error más descriptivos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForcePasswordChangeRequest {

    /**
     * Contraseña temporal actual (la que recibió por email).
     */
    @NotBlank(message = "La contraseña actual es requerida")
    private String currentPassword;

    /**
     * Nueva contraseña.
     * Debe cumplir con:
     * - Mínimo 8 caracteres
     * - Al menos una mayúscula
     * - Al menos una minúscula
     * - Al menos un número
     * Las validaciones se realizan en el servicio para mensajes personalizados.
     */
    @NotBlank(message = "La nueva contraseña es requerida")
    private String newPassword;
}
