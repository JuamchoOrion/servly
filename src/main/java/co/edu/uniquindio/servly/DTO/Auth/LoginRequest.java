package co.edu.uniquindio.servly.DTO.Auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    /**
     * Token de reCAPTCHA v2 obtenido del frontend.
     * Campo requerido cuando reCAPTCHA está habilitado.
     */
    private String recaptchaToken;
}
