package co.edu.uniquindio.servly.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando se intenta usar una contraseña que es igual a la actual o temporal.
 * 
 * HTTP 400 - Bad Request
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SamePasswordException extends AuthException {

    public SamePasswordException(String message) {
        super(message);
    }

    public SamePasswordException(String message, Throwable cause) {
        super(message, cause);
    }
}
