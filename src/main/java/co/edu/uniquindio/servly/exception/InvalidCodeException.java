package co.edu.uniquindio.servly.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando un código de verificación (2FA o reset password) es inválido.
 * 
 * HTTP 401 - Unauthorized
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidCodeException extends AuthException {

    public InvalidCodeException(String message) {
        super(message);
    }

    public InvalidCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
