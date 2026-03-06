package co.edu.uniquindio.servly.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando se requiere verificación 2FA pero no se ha proporcionado.
 * 
 * HTTP 428 - Precondition Required
 */
@ResponseStatus(HttpStatus.PRECONDITION_REQUIRED)
public class TwoFactorRequiredException extends AuthException {

    public TwoFactorRequiredException(String message) {
        super(message);
    }

    public TwoFactorRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
