package co.edu.uniquindio.servly.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa la respuesta de Google reCAPTCHA v2.
 *
 * Mapea la respuesta JSON de: https://www.google.com/recaptcha/api/siteverify
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecaptchaResponse {

    /**
     * Indica si la verificación fue exitosa
     */
    private boolean success;

    /**
     * Timestamp de cuando se completó la validación (milisegundos desde epoch)
     */
    @JsonProperty("challenge_ts")
    private String challengeTimestamp;

    /**
     * El nombre de host del sitio donde se ejecutó reCAPTCHA
     */
    private String hostname;

    /**
     * Array de códigos de error si la validación falló
     */
    @JsonProperty("error-codes")
    private String[] errorCodes;

    /**
     * Puntuación de 0.0 a 1.0 (solo para reCAPTCHA v3)
     */
    private Double score;

    /**
     * Acción ejecutada (solo para reCAPTCHA v3)
     */
    private String action;
}

