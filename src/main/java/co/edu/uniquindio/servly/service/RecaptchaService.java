package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Auth.RecaptchaResponse;
import co.edu.uniquindio.servly.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para verificar tokens de reCAPTCHA v2 con Google.
 *
 * Documentación: https://developers.google.com/recaptcha/docs/verify
 *
 * Flujo:
 * 1. El frontend renderiza el widget de reCAPTCHA v2
 * 2. El usuario completa el captcha
 * 3. Google retorna un token (recaptchaToken)
 * 4. El frontend envía este token al backend en la solicitud de login
 * 5. El backend verifica el token con Google
 * 6. Si es válido, procede con la autenticación; si no, rechaza la solicitud
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecaptchaService {

    private final RestTemplate restTemplate;

    @Value("${app.recaptcha.secret-key}")
    private String recaptchaSecretKey;

    @Value("${app.recaptcha.enabled:true}")
    private boolean recaptchaEnabled;

    /**
     * URL del endpoint de Google para verificar reCAPTCHA v2
     */
    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    /**
     * Verifica un token de reCAPTCHA v2 con Google.
     *
     * @param token Token recibido del frontend
     * @return true si el captcha es válido; false en caso contrario
     * @throws AuthException si la solicitud falla o la respuesta es inválida
     */
    public boolean verifyToken(String token) {
        // Si reCAPTCHA está deshabilitado, pasar la validación
        if (!recaptchaEnabled) {
            log.debug("reCAPTCHA está deshabilitado, saltando validación");
            return true;
        }

        // Validar que el token no esté vacío
        if (token == null || token.trim().isEmpty()) {
            log.warn("Token de reCAPTCHA vacío o nulo");
            throw new AuthException("Token de reCAPTCHA requerido");
        }

        try {
            log.debug("Verificando token de reCAPTCHA con Google");

            // Preparar parámetros de la solicitud
            Map<String, String> params = new HashMap<>();
            params.put("secret", recaptchaSecretKey);
            params.put("response", token);

            // Construir la URL con parámetros
            String urlWithParams = UriComponentsBuilder.fromHttpUrl(RECAPTCHA_VERIFY_URL)
                    .queryParam("secret", recaptchaSecretKey)
                    .queryParam("response", token)
                    .toUriString();

            // Realizar la solicitud POST a Google
            ResponseEntity<RecaptchaResponse> response = restTemplate.postForEntity(
                    urlWithParams,
                    new HttpEntity<>(getHeaders()),
                    RecaptchaResponse.class
            );

            RecaptchaResponse recaptchaResponse = response.getBody();

            if (recaptchaResponse == null) {
                log.error("Respuesta nula de Google reCAPTCHA");
                throw new AuthException("No se pudo verificar reCAPTCHA");
            }

            if (!recaptchaResponse.isSuccess()) {
                log.warn("Validación de reCAPTCHA fallida. Códigos de error: {}",
                        String.join(", ", recaptchaResponse.getErrorCodes() != null ?
                                recaptchaResponse.getErrorCodes() : new String[]{"desconocido"}));
                return false;
            }

            log.debug("Token de reCAPTCHA válido. Hostname: {}", recaptchaResponse.getHostname());
            return true;

        } catch (RestClientException e) {
            log.error("Error al comunicarse con Google reCAPTCHA: {}", e.getMessage(), e);
            throw new AuthException("Error al verificar reCAPTCHA: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al verificar reCAPTCHA: {}", e.getMessage(), e);
            throw new AuthException("Error al verificar reCAPTCHA");
        }
    }

    /**
     * Obtiene los headers necesarios para la solicitud a Google.
     */
    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }
}

