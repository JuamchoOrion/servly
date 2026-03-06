package co.edu.uniquindio.servly.handlers;

import co.edu.uniquindio.servly.service.AuthService;
import co.edu.uniquindio.servly.service.OAuth2UserAdapter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Handler ejecutado cuando el login con Google es exitoso.
 * Genera el JWT y redirige al frontend con los tokens en la URL.
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

        if (!(authentication.getPrincipal() instanceof OAuth2UserAdapter oAuth2User)) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        var authResponse = authService.buildAuthResponse(oAuth2User.getUser());
        log.info("Login OAuth2 exitoso para: {}", oAuth2User.getUser().getEmail());

        String targetUrl = UriComponentsBuilder
                .fromUriString(frontendUrl + "/oauth2/callback")
                .queryParam("accessToken",  authResponse.getAccessToken())
                .queryParam("refreshToken", authResponse.getRefreshToken())
                .queryParam("name",         authResponse.getName())
                .queryParam("role",         authResponse.getRole())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}