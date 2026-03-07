package co.edu.uniquindio.servly.handlers;

import co.edu.uniquindio.servly.model.entity.User;
import co.edu.uniquindio.servly.service.AuthService;
import co.edu.uniquindio.servly.service.OAuth2UserAdapter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Handler ejecutado cuando el login con Google es exitoso.
 * Para desarrollo local: pasa los tokens en la URL.
 * Para producción: usar cookies SameSite=None; Secure.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final ApplicationContext applicationContext;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response, Authentication authentication) throws IOException {

        AuthService authService = applicationContext.getBean(AuthService.class);

        User user = extractUserFromAuthentication(authentication);

        if (user == null) {
            log.error("No se pudo extraer el usuario de la autenticación: {}", authentication.getPrincipal().getClass());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        var authResponse = authService.buildAuthResponse(user);
        log.info("Login OAuth2 exitoso para: {}", user.getEmail());

        // ⚠️ Para desarrollo local: pasar tokens en la URL
        // Las cookies Set-Cookie en redirecciones cross-origin (8081 → 4200) no son guardadas por el navegador
        // En producción con HTTPS, usar cookies SameSite=None; Secure
        String targetUrl = UriComponentsBuilder
                .fromUriString(frontendUrl + "/oauth2/callback")
                .queryParam("accessToken", authResponse.getAccessToken())
                .queryParam("refreshToken", authResponse.getRefreshToken())
                .queryParam("email", user.getEmail())
                .queryParam("name", user.getName())
                .queryParam("role", user.getRole().name())
                .build().toUriString();

        log.info("Redirigiendo a: {}", targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * Extrae el usuario User desde la autenticación.
     * Soporta OAuth2UserAdapter que implementa OidcUser.
     */
    private User extractUserFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        // Caso 1: OAuth2UserAdapter (implementa OidcUser)
        if (principal instanceof OAuth2UserAdapter adapter) {
            return adapter.getUser();
        }

        // Caso 2: OAuth2AuthenticationToken con OAuth2UserAdapter
        if (authentication instanceof OAuth2AuthenticationToken token) {
            OAuth2User oauth2User = token.getPrincipal();
            if (oauth2User instanceof OAuth2UserAdapter adapter) {
                return adapter.getUser();
            }
        }

        log.error("Tipo de principal no soportado: {}", principal.getClass());
        return null;
    }
}