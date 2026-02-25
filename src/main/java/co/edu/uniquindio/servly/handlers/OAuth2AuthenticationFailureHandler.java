package co.edu.uniquindio.servly.handlers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Handler ejecutado cuando el login con Google falla.
 * Redirige al frontend con el mensaje de error en la URL.
 */
@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response, AuthenticationException exception) throws IOException {

        log.warn("Fallo en autenticación OAuth2: {}", exception.getMessage());

        String error = exception.getMessage() != null
                ? exception.getMessage()
                : "Error durante la autenticación con Google";

        String targetUrl = UriComponentsBuilder
                .fromUriString(frontendUrl + "/login")
                .queryParam("error", URLEncoder.encode(error, StandardCharsets.UTF_8))
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}