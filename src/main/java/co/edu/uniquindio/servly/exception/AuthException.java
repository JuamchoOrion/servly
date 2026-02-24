package co.edu.uniquindio.servly.exception;

/**
 * Excepción base para errores de autenticación.
 * Las subclases definen el código HTTP específico.
 */
public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
