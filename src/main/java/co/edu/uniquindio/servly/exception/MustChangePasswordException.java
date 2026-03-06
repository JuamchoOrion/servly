package co.edu.uniquindio.servly.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando un usuario intenta acceder a recursos
 * pero debe cambiar su contraseña primero.
 * 
 * HTTP 428 - Precondition Required
 */
@ResponseStatus(HttpStatus.PRECONDITION_REQUIRED)
public class MustChangePasswordException extends AuthException {

    public MustChangePasswordException(String message) {
        super(message);
    }

    public MustChangePasswordException(String message, Throwable cause) {
        super(message, cause);
    }
}
