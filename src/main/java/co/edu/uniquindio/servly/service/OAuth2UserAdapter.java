package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.model.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;

/**
 * Adaptador que envuelve la entidad User para que sea compatible con OidcUser.
 * Permite acceder al User real desde el OAuth2AuthenticationSuccessHandler.
 */
public class OAuth2UserAdapter implements OidcUser {

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

    // Métodos de OidcUser/StandardClaimAccessor

    @Override
    public Map<String, Object> getClaims() {
        return attributes;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return new OidcUserInfo(attributes);
    }

    @Override
    public OidcIdToken getIdToken() {
        String idTokenString = (String) attributes.get("id_token");
        if (idTokenString != null) {
            return new OidcIdToken(idTokenString, null, null, attributes);
        }
        return new OidcIdToken("fake", null, null, attributes);
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public Boolean getEmailVerified() {
        return (Boolean) attributes.get("email_verified");
    }

    @Override
    public String getPhoneNumber() {
        return (String) attributes.get("phone_number");
    }

    @Override
    public Boolean getPhoneNumberVerified() {
        return (Boolean) attributes.get("phone_number_verified");
    }

    public String getNickname() {
        return (String) attributes.get("nickname");
    }

    public String getProfile() {
        return (String) attributes.get("profile");
    }

    @Override
    public String getPicture() {
        return (String) attributes.get("picture");
    }

    @Override
    public String getGivenName() {
        return (String) attributes.get("given_name");
    }

    @Override
    public String getFamilyName() {
        return (String) attributes.get("family_name");
    }

    @Override
    public String getLocale() {
        return (String) attributes.get("locale");
    }

    @Override
    public Instant getUpdatedAt() {
        Object updatedAt = attributes.get("updated_at");
        if (updatedAt instanceof Instant instant) {
            return instant;
        }
        if (updatedAt instanceof Long timestamp) {
            return Instant.ofEpochSecond(timestamp);
        }
        if (updatedAt instanceof Integer timestamp) {
            return Instant.ofEpochSecond(timestamp);
        }
        return null;
    }

    public String getZoneinfo() {
        return (String) attributes.get("zoneinfo");
    }
}