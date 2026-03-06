package co.edu.uniquindio.servly.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando un usuario intenta usar Google OAuth2 pero tiene bloqueado
 * el acceso porque debe completar su primer login con contraseña tradicional.
 * 
 * HTTP 403 - Forbidden
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class GoogleOAuth2BlockedException extends AuthException {

    public GoogleOAuth2BlockedException(String message) {
        super(message);
    }

    public GoogleOAuth2BlockedException(String message, Throwable cause) {
        super(message, cause);
    }
}
