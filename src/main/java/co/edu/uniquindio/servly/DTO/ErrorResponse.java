package co.edu.uniquindio.servly.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Respuesta de error estandarizada para todos los endpoints.
 * Permite al frontend procesar errores de forma consistente.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Código HTTP del error
     * Ejemplo: 400, 401, 403, 422, 500
     */
    private int status;

    /**
     * Mensaje de error legible para el usuario
     * Ejemplo: "Email o contraseña incorrectos"
     */
    private String message;

    /**
     * Tipo de error (para categorizar en frontend)
     * Ejemplo: "INVALID_CREDENTIALS", "RECAPTCHA_FAILED", "VALIDATION_ERROR"
     */
    private String errorType;

    /**
     * Detalles adicionales del error
     * Útil para errores de validación múltiples
     * Ejemplo: {"email": "Email ya registrado", "password": "Muy débil"}
     */
    private Map<String, String> details;

    /**
     * Timestamp cuando ocurrió el error
     */
    private LocalDateTime timestamp;

    /**
     * Path del endpoint que causó el error
     * Ejemplo: "/api/auth/login"
     */
    private String path;

    /**
     * Constructor para errores simples
     */
    public ErrorResponse(int status, String message, String errorType) {
        this.status = status;
        this.message = message;
        this.errorType = errorType;
        this.timestamp = LocalDateTime.now();
    }
}

