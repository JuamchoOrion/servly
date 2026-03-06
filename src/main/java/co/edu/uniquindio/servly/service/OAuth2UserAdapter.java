package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.model.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * Adaptador que envuelve la entidad User para que sea compatible con OAuth2User.
 * Permite acceder al User real desde el OAuth2AuthenticationSuccessHandler.
 */
public class OAuth2UserAdapter implements OAuth2User {

    private final User                user;
    private final Map<String, Object> attributes;

    public OAuth2UserAdapter(User user, Map<String, Object> attributes) {
        this.user       = user;
        this.attributes = attributes;
    }

    public User getUser() { return user; }

    @Override
    public Map<String, Object> getAttributes() { return attributes; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getAuthorities();
    }

    @Override
    public String getName() { return user.getProviderId(); }
}