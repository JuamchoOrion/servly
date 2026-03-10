package co.edu.uniquindio.servly.exception;

/**
 * Excepción para representar conflictos de recursos (HTTP 409).
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
