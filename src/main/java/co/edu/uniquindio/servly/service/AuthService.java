package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.*;
import co.edu.uniquindio.servly.DTO.Auth.AuthResponse;
import co.edu.uniquindio.servly.DTO.Auth.LoginRequest;
import co.edu.uniquindio.servly.DTO.Auth.RefreshTokenRequest;
import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.DTO.Password.ForcePasswordChangeRequest;
import co.edu.uniquindio.servly.DTO.Password.ForgotPasswordRequest;
import co.edu.uniquindio.servly.DTO.Password.ResetPasswordRequest;
import co.edu.uniquindio.servly.DTO.Roles.UserResponse;
import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.exception.MustChangePasswordException;
import co.edu.uniquindio.servly.exception.SamePasswordException;
import co.edu.uniquindio.servly.exception.WeakPasswordException;
import co.edu.uniquindio.servly.model.entity.AuditLog;
import co.edu.uniquindio.servly.model.entity.RevokedToken;
import co.edu.uniquindio.servly.model.entity.User;
import co.edu.uniquindio.servly.model.enums.AuthProvider;
import co.edu.uniquindio.servly.model.enums.CodeType;
import co.edu.uniquindio.servly.repository.RevokedTokenRepository;
import co.edu.uniquindio.servly.repository.UserRepository;
import co.edu.uniquindio.servly.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

/**
 * Servicio principal de autenticación de Servly.
 *
 * Flujos:
 *  1. Login con JWT (con y sin 2FA)
 *  2. Verificación de código 2FA
 *  3. Recuperación de contraseña
 *  4. Renovación de token (refresh)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository           userRepository;
    private final PasswordEncoder          passwordEncoder;
    private final JwtTokenProvider         jwtTokenProvider;
    private final AuthenticationManager    authenticationManager;
    private final VerificationCodeService  codeService;
    private final EmailService             emailService;
    private final RevokedTokenRepository   revokedTokenRepository;
    private final RecaptchaService         recaptchaService;
    private final AuditService             auditService;
    private final co.edu.uniquindio.servly.metrics.AuthMetricsService authMetricsService;

    // ── Login ─────────────────────────────────────────────────────────────────

    public Object login(LoginRequest request) {
        String operationKey = "login_" + request.getEmail() + "_" + System.currentTimeMillis();
        auditService.startOperation(operationKey);

        // Obtener usuario para tener el rol (incluso si falla el login)
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        String role = user != null ? user.getRole().name() : null;

        // Validar reCAPTCHA antes de intentar la autenticación
        if (!recaptchaService.verifyToken(request.getRecaptchaToken())) {
            log.warn("Intento de login fallido por reCAPTCHA inválido para: {}", request.getEmail());
            authMetricsService.recordLoginAttempt(role, false, 0);
            auditService.endOperation(operationKey, AuditLog.EVENT_LOGIN_FAILED, request.getEmail(),
                    role, false, "reCAPTCHA inválido", null);
            throw new AuthException("Validación de reCAPTCHA fallida. Por favor, intenta nuevamente.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), request.getPassword()));
        } catch (AuthenticationException e) {
            authMetricsService.recordLoginAttempt(role, false, 0);
            auditService.endOperation(operationKey, AuditLog.EVENT_LOGIN_FAILED, request.getEmail(),
                    role, false, e.getMessage(), null);
            throw new AuthException("Email o contraseña incorrectos");
        }

        if (user == null) {
            throw new AuthException("Usuario no encontrado");
        }

        // Si el usuario debe cambiar contraseña (primer login), requerir 2FA
        if (user.isMustChangePassword() && !user.isFirstLoginCompleted()) {
            log.info("Login de usuario con primer login pendiente: {}", user.getEmail());
            // Generar y enviar código 2FA para el primer login
            String code = codeService.generateAndSave(user.getEmail(), CodeType.TWO_FACTOR);
            emailService.sendTwoFactorCode(user.getEmail(), user.getName(), code);
            log.info("Código 2FA enviado a: {}", user.getEmail());

            auditService.logEvent(AuditLog.EVENT_2FA_CODE_SENT, user.getEmail(),
                    user.getRole().name(), true, null, null, null);
            auditService.endOperation(operationKey, AuditLog.EVENT_LOGIN_REQUEST, user.getEmail(),
                    user.getRole().name(), true, null, null);

            return new MessageResponse(
                    "Verificación en 2 pasos requerida. Se envió un código a tu correo electrónico.");
        }

        // Login normal sin 2FA (después del primer login ya no se pide 2FA)
        long startTime = System.currentTimeMillis();
        AuthResponse response = buildAuthResponse(user);
        long duration = System.currentTimeMillis() - startTime;

        authMetricsService.recordLoginAttempt(user.getRole().name(), true, duration);

        auditService.endOperation(operationKey, AuditLog.EVENT_LOGIN_SUCCESS, user.getEmail(),
                user.getRole().name(), true, null, user.getId());
        auditService.logSessionStarted(user.getEmail(), user.getRole().name(),
                "session_" + user.getId() + "_" + System.currentTimeMillis());

        return response;
    }

    // ── Verificación 2FA ──────────────────────────────────────────────────────

    /**
     * Verifica el código 2FA.
     * - Si es primer login (mustChangePassword), retorna un flag para forzar cambio de contraseña
     * - Si no, retorna los tokens de acceso normalmente
     */
    public AuthResponse verifyTwoFactor(TwoFactorRequest request) {
        String operationKey = "2fa_" + request.getEmail() + "_" + System.currentTimeMillis();
        auditService.startOperation(operationKey);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("Usuario no encontrado"));

        try {
            long startTime = System.currentTimeMillis();
            codeService.verifyCode(request.getEmail(), request.getCode(), CodeType.TWO_FACTOR);
            long duration = System.currentTimeMillis() - startTime;

            authMetricsService.record2FAVerification(duration);
            log.info("2FA verificado para: {}", request.getEmail());

            auditService.endOperation(operationKey, AuditLog.EVENT_2FA_VERIFICATION_SUCCESS, 
                    user.getEmail(), user.getRole().name(), true, null, user.getId());
            auditService.logSessionStarted(user.getEmail(), user.getRole().name(),
                    "session_" + user.getId() + "_" + System.currentTimeMillis());

            // Si es primer login, el frontend deberá redirigir al cambio de contraseña
            // Los tokens se entregan pero el usuario debe cambiar la contraseña
            return buildAuthResponse(user);
        } catch (AuthException e) {
            auditService.endOperation(operationKey, AuditLog.EVENT_2FA_VERIFICATION_FAILED,
                    user.getEmail(), user.getRole().name(), false, e.getMessage(), null);
            throw e;
        }
    }

    // ── Recuperación de contraseña ────────────────────────────────────────────

    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            if (user.getProvider() != AuthProvider.LOCAL) return;
            
            auditService.logEvent(AuditLog.EVENT_PASSWORD_RECOVERY_REQUEST, user.getEmail(),
                    user.getRole().name(), true, null, null, null);
            
            String code = codeService.generateAndSave(user.getEmail(), CodeType.PASSWORD_RESET);
            emailService.sendPasswordResetCode(user.getEmail(), user.getName(), code);
            log.info("Código de reset enviado a: {}", user.getEmail());
            
            auditService.logEvent(AuditLog.EVENT_PASSWORD_RECOVERY_CODE_SENT, user.getEmail(),
                    user.getRole().name(), true, null, null, null);
        });
        return new MessageResponse(
                "Si ese email está registrado, recibirás un código para restablecer tu contraseña.");
    }

    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String operationKey = "password_reset_" + request.getEmail() + "_" + System.currentTimeMillis();
        auditService.startOperation(operationKey);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("Usuario no encontrado"));

        if (user.getProvider() != AuthProvider.LOCAL) {
            auditService.endOperation(operationKey, AuditLog.EVENT_PASSWORD_RESET_FAILED,
                    user.getEmail(), null, false, "OAuth2 user", null);
            throw new AuthException("Esta cuenta usa inicio de sesión con Google");
        }

        try {
            codeService.verifyCode(request.getEmail(), request.getCode(), CodeType.PASSWORD_RESET);
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            user.setPasswordVersion(user.getPasswordVersion() + 1);  // Incrementar versión
            userRepository.save(user);

            log.info("Contraseña restablecida para: {}", request.getEmail());
            
            auditService.endOperation(operationKey, AuditLog.EVENT_PASSWORD_RESET_SUCCESS,
                    user.getEmail(), user.getRole().name(), true, null, user.getId());
            
            return new MessageResponse("Contraseña actualizada exitosamente. Ya puedes iniciar sesión.");
        } catch (AuthException e) {
            auditService.endOperation(operationKey, AuditLog.EVENT_PASSWORD_RESET_FAILED,
                    user.getEmail(), user.getRole().name(), false, e.getMessage(), null);
            throw e;
        }
    }

    // ── Refresh token ─────────────────────────────────────────────────────────

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String email;
        try {
            email = jwtTokenProvider.extractUsername(request.getRefreshToken());
        } catch (Exception e) {
            throw new AuthException("Refresh token inválido o expirado");
        }

        // Verificar si el token está en la blacklist
        if (revokedTokenRepository.existsByToken(request.getRefreshToken())) {
            throw new AuthException("El token ha sido revocado. Por favor inicie sesión de nuevo");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("Usuario no encontrado"));

        if (!jwtTokenProvider.isTokenValid(request.getRefreshToken(), user)) {
            throw new AuthException("Refresh token inválido o expirado");
        }

        return buildAuthResponse(user);
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    /**
     * Invalida un refresh token agregándolo a la blacklist.
     * El usuario deberá autenticarse de nuevo para obtener nuevos tokens.
     */
    @Transactional
    public MessageResponse logout(RefreshTokenRequest request) {
        String email;
        try {
            email = jwtTokenProvider.extractUsername(request.getRefreshToken());
        } catch (Exception e) {
            // Si no se puede extraer el email, igual agregamos el token a la blacklist
            email = "unknown";
        }

        // Extraer la fecha de expiración del token
        LocalDateTime expiresAt;
        try {
            Date expirationDate = jwtTokenProvider.extractExpiration(request.getRefreshToken());
            expiresAt = LocalDateTime.ofInstant(
                expirationDate.toInstant(), 
                java.time.ZoneId.systemDefault()
            );
        } catch (Exception e) {
            // Si no se puede extraer, usamos una fecha por defecto (24 horas)
            expiresAt = LocalDateTime.now().plusHours(24);
        }

        // Guardar en la blacklist
        RevokedToken revokedToken = RevokedToken.builder()
                .token(request.getRefreshToken())
                .userEmail(email)
                .expiresAt(expiresAt)
                .build();

        revokedTokenRepository.save(revokedToken);

        log.info("Token revocado para usuario: {} (expires: {})", email, expiresAt);

        return new MessageResponse("Sesión cerrada exitosamente");
    }

    // ── Utilidad pública ─────────────────────────────────────────────────────

    public AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .accessToken(jwtTokenProvider.generateAccessToken(user))
                .refreshToken(jwtTokenProvider.generateRefreshToken(user))
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .mustChangePassword(user.isMustChangePassword())
                .firstLoginCompleted(user.isFirstLoginCompleted())
                .build();
    }

    // ── Cambio Forzado de Contraseña (Primer Login) ─────────────────────────

    /**
     * Método para cambiar la contraseña cuando mustChangePassword = true.
     * Solo permite este cambio si el usuario está en estado de primer login.
     */
    public AuthResponse forcePasswordChange(ForcePasswordChangeRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("Usuario no encontrado"));

        // Verificar que el usuario debe cambiar password
        if (!user.isMustChangePassword()) {
            throw new MustChangePasswordException(
                "Este usuario no requiere cambio de contraseña");
        }

        // Verificar que la contraseña actual (temporal) sea correcta
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AuthException("Contraseña actual incorrecta");
        }

        // Validar que la nueva contraseña no sea igual a la temporal
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new SamePasswordException(
                "La nueva contraseña debe ser diferente a la contraseña actual");
        }

        // Validar fortaleza de la contraseña
        validatePasswordStrength(request.getNewPassword());

        // Actualizar contraseña
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        user.setFirstLoginCompleted(true);
        user.setTwoFactorEnabled(false);  // Desactivar 2FA por defecto en primer login
        user.setFirstLoginAt(LocalDateTime.now());
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setPasswordVersion(user.getPasswordVersion() + 1);  // Incrementar versión

        userRepository.save(user);

        log.info("Password cambiado exitosamente para usuario: {}", email);

        return buildAuthResponse(user);
    }

    /**
     * Valida que la contraseña cumpla con los requisitos mínimos de seguridad.
     */
    private void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new WeakPasswordException(
                "La contraseña debe tener al menos 8 caracteres");
        }
        
        if (!password.matches(".*[A-Z].*")) {
            throw new WeakPasswordException(
                "La contraseña debe contener al menos una letra mayúscula");
        }
        
        if (!password.matches(".*[a-z].*")) {
            throw new WeakPasswordException(
                "La contraseña debe contener al menos una letra minúscula");
        }
        
        if (!password.matches(".*\\d.*")) {
            throw new WeakPasswordException(
                "La contraseña debe contener al menos un número");
        }
    }

    // ── Obtener usuario actual ───────────────────────────────────────────────

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("Usuario no encontrado"));

        return UserResponse.from(user);
    }
}