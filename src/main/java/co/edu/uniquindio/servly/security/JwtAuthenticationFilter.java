package co.edu.uniquindio.servly.security;

import co.edu.uniquindio.servly.exception.AccountDisabledException;
import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.model.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Intercepta cada request y valida el JWT de usuario (staff).
 * Si el token es válido, inyecta la autenticación en el SecurityContext.
 * No actúa sobre /api/client/** (esas rutas las maneja TableSessionFilter).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider    jwtTokenProvider;
    private final UserDetailsService  userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        String userEmail;

        try {
            userEmail = jwtTokenProvider.extractUsername(jwt);
        } catch (Exception e) {
            log.debug("No se pudo extraer username del token: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            // Validaciones adicionales si es una instancia de User
            if (userDetails instanceof User user) {
                validateUserState(user);
            }

            if (jwtTokenProvider.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("Usuario autenticado via JWT: {}", userEmail);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Valida el estado de la cuenta del usuario.
     * Lanza excepciones apropiadas según el estado.
     */
    private void validateUserState(User user) {
        if (!user.isEnabled()) {
            log.warn("Intento de acceso con cuenta deshabilitada: {}", user.getEmail());
            throw new AccountDisabledException(
                "La cuenta está deshabilitada. Contacte al administrador.");
        }

        if (!user.isAccountNonLocked()) {
            log.warn("Intento de acceso con cuenta bloqueada: {}", user.getEmail());
            throw new AccountDisabledException(
                "La cuenta está bloqueada temporalmente. Intente más tarde.");
        }

        if (!user.isCredentialsNonExpired()) {
            log.warn("Intento de acceso con contraseña expirada: {}", user.getEmail());
            throw new AuthException(
                "Su contraseña ha expirado. Solicite un restablecimiento.");
        }

        log.debug("Usuario {} tiene estado válido", user.getEmail());
    }
}