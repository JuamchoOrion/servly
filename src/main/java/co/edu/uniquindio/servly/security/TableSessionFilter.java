package co.edu.uniquindio.servly.security;

import co.edu.uniquindio.servly.repository.TableSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro exclusivo para /api/client/**
 *
 * Valida el sessionToken de mesa y, si es correcto,
 * inyecta ROLE_CLIENT en el SecurityContext.
 * El cliente nunca necesita una cuenta de usuario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TableSessionFilter extends OncePerRequestFilter {

    private final TableJwtProvider       tableJwtProvider;
    private final TableSessionRepository tableSessionRepository;

    private static final String CLIENT_PATH = "/api/client/";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith(CLIENT_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Obtener token de Cookie o Header Authorization
        String token = null;

        // Intentar obtener de Cookie primero
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("sessionToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // Si no está en cookie, intentar obtener del header Authorization
        if (token == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (token == null) {
            log.debug("No se encontró sessionToken en cookie o Authorization header");
            filterChain.doFilter(request, response);
            return;
        }


        if (!tableJwtProvider.isValidTableToken(token)) {
            log.debug("Token en /api/client/ no es TABLE_SESSION");
            filterChain.doFilter(request, response);
            return;
        }

        String  sessionId   = tableJwtProvider.extractSessionId(token);
        Integer tableNumber = tableJwtProvider.extractTableNumber(token);

        boolean sessionExists = tableSessionRepository
                .findBySessionTokenAndActiveTrue(token)
                .isPresent();

        if (!sessionExists) {
            log.debug("Sesión de mesa {} no encontrada o inactiva", tableNumber);
            filterChain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        "table:" + tableNumber,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))
                );
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
        log.debug("Sesión de mesa {} autenticada como CLIENT", tableNumber);

        filterChain.doFilter(request, response);
    }
}