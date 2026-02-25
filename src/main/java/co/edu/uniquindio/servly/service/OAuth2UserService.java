package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.exception.GoogleOAuth2BlockedException;
import co.edu.uniquindio.servly.model.entity.User;
import co.edu.uniquindio.servly.model.enums.AuthProvider;
import co.edu.uniquindio.servly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Procesa el usuario devuelto por Google tras el login OAuth2.
 *
 * Flujos:
 *  - Email existente (LOCAL o GOOGLE) con mustChangePassword = false → permite login
 *  - Email existente con mustChangePassword = true → BLOQUEA (debe completar primer login)
 *  - Email NO existe → rechaza (solo admin puede crear cuentas)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(request);

        if (!"google".equals(request.getClientRegistration().getRegistrationId())) {
            throw new OAuth2AuthenticationException("Proveedor OAuth2 no soportado");
        }

        return processGoogleUser(oAuth2User);
    }

    private OAuth2User processGoogleUser(OAuth2User oAuth2User) {
        String email     = oAuth2User.getAttribute("email");
        String name      = oAuth2User.getAttribute("name");
        String googleSub = oAuth2User.getAttribute("sub");

        if (email == null) throw new AuthException("No se pudo obtener el email desde Google");

        User user = userRepository.findByEmail(email)
                .map(existing -> updateExisting(existing, name, googleSub))
                .orElseGet(() -> createNew(email, name, googleSub));

        userRepository.save(user);
        return new OAuth2UserAdapter(user, oAuth2User.getAttributes());
    }

    private User updateExisting(User user, String name, String providerId) {
        // BLOQUEO: Si debe cambiar password, no permitir Google
        if (user.isMustChangePassword()) {
            log.warn("Intento de login OAuth2 bloqueado para usuario {} que debe cambiar contraseña", user.getEmail());
            throw new GoogleOAuth2BlockedException(
                "Complete su primer inicio de sesión con contraseña antes de usar Google. " +
                "Su administrador le envió credenciales temporales a su correo."
            );
        }

        // ✅ PERMITIR: Usuario LOCAL que ya completó primer login puede usar Google
        // Si el provider era LOCAL, lo actualizamos a GOOGLE para mantener consistencia
        if (user.getProvider() == AuthProvider.LOCAL) {
            log.info("Usuario LOCAL {} ahora puede usar Google OAuth2", user.getEmail());
            user.setProvider(AuthProvider.GOOGLE);
        }

        user.setName(name);
        user.setProviderId(providerId);
        log.info("Login OAuth2 permitido para usuario existente: {}", user.getEmail());
        return user;
    }

    private User createNew(String email, String name, String providerId) {
        // ❌ RECHAZAR: No crear cuentas automáticas. Solo admin puede crear usuarios.
        log.error("Intento de login OAuth2 con email no registrado: {}", email);
        throw new AuthException(
            "El email " + email + " no está registrado en el sistema. " +
            "Un administrador debe crear tu cuenta primero."
        );
    }
}