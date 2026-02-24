package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.exception.GoogleOAuth2BlockedException;
import co.edu.uniquindio.servly.model.entity.User;
import co.edu.uniquindio.servly.model.enums.AuthProvider;
import co.edu.uniquindio.servly.model.enums.Role;
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
 *  - Email nuevo          → crea cuenta con rol WAITER (ajustar según necesidad)
 *  - Email existente LOCAL → rechaza con error (tiene cuenta con contraseña)
 *  - Email existente GOOGLE → actualiza nombre y providerId
 *  - Email con mustChangePassword = true → BLOQUEA (debe completar primer login)
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
        
        if (user.getProvider() == AuthProvider.LOCAL) {
            throw new AuthException(
                "Este email ya tiene cuenta con contraseña. Inicia sesión con tu email.");
        }
        user.setName(name);
        user.setProviderId(providerId);
        return user;
    }

    private User createNew(String email, String name, String providerId) {
        log.info("Nuevo usuario creado via Google OAuth2: {}", email);
        return User.builder()
                .email(email)
                .name(name)
                .role(Role.WAITER)          // Ajustar rol por defecto según política
                .provider(AuthProvider.GOOGLE)
                .providerId(providerId)
                .twoFactorEnabled(false)
                .enabled(true)
                .mustChangePassword(false)
                .firstLoginCompleted(true)
                .build();
    }
}