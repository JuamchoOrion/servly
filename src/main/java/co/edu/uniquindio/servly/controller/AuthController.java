package co.edu.uniquindio.servly.controller;

import co.edu.uniquindio.servly.DTO.*;
import co.edu.uniquindio.servly.DTO.AuthResponse;
import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.DTO.UserResponse;
import co.edu.uniquindio.servly.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de autenticación para el staff.
 *
 * Públicos:
 *   POST /api/auth/login
 *   POST /api/auth/verify-2fa
 *   POST /api/auth/forgot-password
 *   POST /api/auth/reset-password
 *   POST /api/auth/refresh-token
 *
 * Protegidos:
 *   GET  /api/auth/me
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-2fa")
    public AuthResponse verifyTwoFactor(@Valid @RequestBody TwoFactorRequest request) {
        return authService.verifyTwoFactor(request);
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
    public AuthResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponse user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }
}