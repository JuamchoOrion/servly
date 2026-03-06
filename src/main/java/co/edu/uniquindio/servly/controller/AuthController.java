package co.edu.uniquindio.servly.controller;

import co.edu.uniquindio.servly.DTO.*;
import co.edu.uniquindio.servly.DTO.AuthResponse;
import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.DTO.UserResponse;
import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import co.edu.uniquindio.servly.exception.MustChangePasswordException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import co.edu.uniquindio.servly.util.CookieUtil;

import java.util.Map;

/**
 * Endpoints de autenticación para el staff.
 *
 * Públicos:
 *   POST /api/auth/login
 *   POST /api/auth/verify-2fa
 *   POST /api/auth/forgot-password
 *   POST /api/auth/reset-password
 *   POST /api/auth/refresh-token
 *   POST /api/auth/logout
 *
 * Protegidos:
 *   GET  /api/auth/me
 *   POST /api/auth/force-password-change
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    /**
     * POST /api/auth/login
     *
     * Autentica un usuario con email, contraseña y verificación de reCAPTCHA.
     *
     * @param request LoginRequest con email, password, recaptchaToken
     * @return AuthResponse con JWT, roles y datos del usuario
     *
     * @throws AuthException 401 si credenciales son inválidas
     * @throws AuthException 400 si reCAPTCHA es inválido
     * @throws MustChangePasswordException si es primer login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        Object result = authService.login(request);

        // Si es primer login con 2FA requerido, retorna mensaje
        if (result instanceof MessageResponse) {
            // En este caso, el frontend debe hacer POST a /api/auth/verify-2fa
            MessageResponse msgResponse = (MessageResponse) result;
            return ResponseEntity.accepted()
                    .header("X-2FA-Required", "true")
                    .body(msgResponse);
        }

        // Login exitoso: retornar AuthResponse
        if (result instanceof AuthResponse) {
            AuthResponse authResponse = (AuthResponse) result;

            // Guardar tokens en cookies
            cookieUtil.addJwtCookie(response, authResponse.getAccessToken(), 86400);
            cookieUtil.addRefreshTokenCookie(response, authResponse.getRefreshToken(), 604800);

            // Retornar datos del usuario (sin tokens en el body)
            return ResponseEntity.ok(Map.of(
                    "userId", authResponse.getUserId(),
                    "email", authResponse.getEmail(),
                    "name", authResponse.getName(),
                    "role", authResponse.getRole(),
                    "mustChangePassword", authResponse.isMustChangePassword(),
                    "firstLoginCompleted", authResponse.isFirstLoginCompleted()
            ));
        }

        // No debería llegar aquí
        throw new AuthException("Respuesta inesperada del servidor");
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<?> verifyTwoFactor(
            @Valid @RequestBody TwoFactorRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.verifyTwoFactor(request);

        // Guardar tokens en cookies
        cookieUtil.addJwtCookie(response, authResponse.getAccessToken(), 86400);
        cookieUtil.addRefreshTokenCookie(response, authResponse.getRefreshToken(), 604800);

        // Retornar datos del usuario (sin tokens en el body)
        return ResponseEntity.ok(Map.of(
                "userId", authResponse.getUserId(),
                "email", authResponse.getEmail(),
                "name", authResponse.getName(),
                "role", authResponse.getRole(),
                "mustChangePassword", authResponse.isMustChangePassword(),
                "firstLoginCompleted", authResponse.isFirstLoginCompleted()
        ));
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.refreshToken(request);

        // Actualizar cookies con nuevos tokens
        cookieUtil.addJwtCookie(response, authResponse.getAccessToken(), 86400);
        cookieUtil.addRefreshTokenCookie(response, authResponse.getRefreshToken(), 604800);

        return ResponseEntity.ok(Map.of(
                "userId", authResponse.getUserId(),
                "email", authResponse.getEmail(),
                "name", authResponse.getName(),
                "role", authResponse.getRole()
        ));
    }

    /**
     * Endpoint para cambiar la contraseña en el primer login.
     * Solo accesible cuando mustChangePassword = true.
     *
     * El usuario debe estar autenticado (haber pasado por login + 2FA)
     * pero con el flag mustChangePassword activo.
     */
    @PostMapping("/force-password-change")
    public ResponseEntity<AuthResponse> forcePasswordChange(
            @Valid @RequestBody ForcePasswordChangeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        AuthResponse response = authService.forcePasswordChange(
            request,
            userDetails.getUsername()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Cierra la sesión del usuario invalidando el refresh token.
     * El token se agrega a la blacklist y no podrá usarse para renovar el acceso.
     *
     * El access token actual seguirá siendo válido hasta su expiración natural,
     * pero no podrá renovarse.
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletResponse response) {

        MessageResponse responseMsg = authService.logout(request);

        // Eliminar cookies
        cookieUtil.removeJwtCookies(response);

        return ResponseEntity.ok(responseMsg);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponse user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }
}