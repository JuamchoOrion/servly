package co.edu.uniquindio.servly.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para forzar el cambio de contraseña en el primer login.
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
     */
    @NotBlank(message = "La nueva contraseña es requerida")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = ".*[A-Z].*", message = "Debe contener al menos una letra mayúscula")
    @Pattern(regexp = ".*[a-z].*", message = "Debe contener al menos una letra minúscula")
    @Pattern(regexp = ".*\\d.*", message = "Debe contener al menos un número")
    private String newPassword;
}
