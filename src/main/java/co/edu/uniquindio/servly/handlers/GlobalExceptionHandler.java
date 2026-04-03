package co.edu.uniquindio.servly.handlers;

import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.exception.*;
import co.edu.uniquindio.servly.exception.ConflictException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Manejador global de excepciones.
 * Centraliza los errores para que todos los controladores retornen
 * respuestas JSON consistentes.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Excepciones de Autenticación ──────────────────────────────────────────

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<MessageResponse> handleAuthException(
            AuthException ex, HttpServletRequest request) {
        log.warn("AuthException en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<MessageResponse> handleBadCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Credenciales inválidas"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<MessageResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("AuthenticationException en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Error de autenticación: " + ex.getMessage()));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<MessageResponse> handleDisabled() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse("La cuenta está deshabilitada"));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<MessageResponse> handleLocked() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse("La cuenta está bloqueada temporalmente"));
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<MessageResponse> handleAccountDisabled(
            AccountDisabledException ex, HttpServletRequest request) {
        log.warn("AccountDisabledException en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse(ex.getMessage()));
    }

    // ── Excepciones Personalizadas ────────────────────────────────────────────

    @ExceptionHandler(MustChangePasswordException.class)
    public ResponseEntity<MessageResponse> handleMustChangePassword(
            MustChangePasswordException ex, HttpServletRequest request) {
        log.warn("MustChangePasswordException en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(TwoFactorRequiredException.class)
    public ResponseEntity<MessageResponse> handleTwoFactorRequired(
            TwoFactorRequiredException ex, HttpServletRequest request) {
        log.warn("TwoFactorRequiredException en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
                .body(new MessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(InvalidCodeException.class)
    public ResponseEntity<MessageResponse> handleInvalidCode(
            InvalidCodeException ex, HttpServletRequest request) {
        log.warn("InvalidCodeException en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ResponseEntity<Map<String, String>> handleWeakPassword(
            WeakPasswordException ex, HttpServletRequest request) {
        log.warn("WeakPasswordException en {}: {}", request.getRequestURI(), ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("newPassword", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(SamePasswordException.class)
    public ResponseEntity<Map<String, String>> handleSamePassword(
            SamePasswordException ex, HttpServletRequest request) {
        log.warn("SamePasswordException en {}: {}", request.getRequestURI(), ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("newPassword", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(GoogleOAuth2BlockedException.class)
    public ResponseEntity<MessageResponse> handleGoogleOAuth2Blocked(
            GoogleOAuth2BlockedException ex, HttpServletRequest request) {
        log.warn("GoogleOAuth2BlockedException en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<MessageResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        log.warn("ConflictException en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new MessageResponse(ex.getMessage()));
    }

    // ── Validaciones ──────────────────────────────────────────────────────────

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

    // ── Errores Genéricos ─────────────────────────────────────────────────────

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<MessageResponse> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {
        log.error("Recurso no encontrado en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error(" Error inesperado en {}: {}", request.getRequestURI(), ex.getMessage());
        log.error("Detalles del error: ", ex);
        return ResponseEntity.internalServerError()
                .body(new MessageResponse("Error interno del servidor: " + ex.getClass().getSimpleName()));
    }
}