package co.edu.uniquindio.servly.handlers;

import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones.
 * Centraliza los errores para que todos los controladores retornen
 * respuestas JSON consistentes.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<MessageResponse> handleAuthException(
            AuthException ex, HttpServletRequest request) {
        log.warn("AuthException en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(new MessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<MessageResponse> handleBadCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Credenciales inválidas"));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<MessageResponse> handleDisabled() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse("La cuenta está deshabilitada"));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<MessageResponse> handleLocked() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse("La cuenta está bloqueada"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field   = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.internalServerError()
                .body(new MessageResponse("Error interno del servidor"));
    }
}