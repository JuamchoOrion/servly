package co.edu.uniquindio.servly.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando una contraseña no cumple con los requisitos de fortaleza.
 * 
 * HTTP 400 - Bad Request
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class WeakPasswordException extends AuthException {

    public WeakPasswordException(String message) {
        super(message);
    }

    public WeakPasswordException(String message, Throwable cause) {
        super(message, cause);
    }
}
