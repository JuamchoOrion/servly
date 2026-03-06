package co.edu.uniquindio.servly.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando una cuenta está deshabilitada o bloqueada.
 * 
 * HTTP 403 - Forbidden
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccountDisabledException extends AuthException {

    public AccountDisabledException(String message) {
        super(message);
    }

    public AccountDisabledException(String message, Throwable cause) {
        super(message, cause);
    }
}
