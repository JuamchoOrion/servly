package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.*;
import co.edu.uniquindio.servly.DTO.AuthResponse;
import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.exception.MustChangePasswordException;
import co.edu.uniquindio.servly.exception.SamePasswordException;
import co.edu.uniquindio.servly.exception.WeakPasswordException;
import co.edu.uniquindio.servly.model.entity.User;
import co.edu.uniquindio.servly.model.enums.AuthProvider;
import co.edu.uniquindio.servly.model.enums.CodeType;
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

    // ── Login ─────────────────────────────────────────────────────────────────

    public Object login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), request.getPassword()));
        } catch (AuthenticationException e) {
            throw new AuthException("Email o contraseña incorrectos");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("Usuario no encontrado"));

        if (user.isTwoFactorEnabled()) {
            String code = codeService.generateAndSave(user.getEmail(), CodeType.TWO_FACTOR);
            emailService.sendTwoFactorCode(user.getEmail(), user.getName(), code);
            log.info("Código 2FA enviado a: {}", user.getEmail());
            return new MessageResponse(
                    "Verificación en 2 pasos requerida. Se envió un código a tu correo electrónico.");
        }

        return buildAuthResponse(user);
    }

    // ── Verificación 2FA ──────────────────────────────────────────────────────

    public AuthResponse verifyTwoFactor(TwoFactorRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("Usuario no encontrado"));

        codeService.verifyCode(request.getEmail(), request.getCode(), CodeType.TWO_FACTOR);
        log.info("2FA verificado para: {}", request.getEmail());
        return buildAuthResponse(user);
    }

    // ── Recuperación de contraseña ────────────────────────────────────────────

    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            if (user.getProvider() != AuthProvider.LOCAL) return;
            String code = codeService.generateAndSave(user.getEmail(), CodeType.PASSWORD_RESET);
            emailService.sendPasswordResetCode(user.getEmail(), user.getName(), code);
            log.info("Código de reset enviado a: {}", user.getEmail());
        });
        return new MessageResponse(
                "Si ese email está registrado, recibirás un código para restablecer tu contraseña.");
    }

    public MessageResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("Usuario no encontrado"));

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new AuthException("Esta cuenta usa inicio de sesión con Google");
        }

        codeService.verifyCode(request.getEmail(), request.getCode(), CodeType.PASSWORD_RESET);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Contraseña restablecida para: {}", request.getEmail());
        return new MessageResponse("Contraseña actualizada exitosamente. Ya puedes iniciar sesión.");
    }

    // ── Refresh token ─────────────────────────────────────────────────────────

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String email;
        try {
            email = jwtTokenProvider.extractUsername(request.getRefreshToken());
        } catch (Exception e) {
            throw new AuthException("Refresh token inválido o expirado");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("Usuario no encontrado"));

        if (!jwtTokenProvider.isTokenValid(request.getRefreshToken(), user)) {
            throw new AuthException("Refresh token inválido o expirado");
        }

        return buildAuthResponse(user);
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
                "La nueva contraseña debe ser diferente a la contraseña temporal");
        }

        // Validar fortaleza de la contraseña
        validatePasswordStrength(request.getNewPassword());

        // Actualizar contraseña
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        user.setFirstLoginCompleted(true);
        user.setFirstLoginAt(LocalDateTime.now());
        user.setPasswordChangedAt(LocalDateTime.now());
        
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