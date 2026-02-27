package co.edu.uniquindio.servly.model.entity;

import co.edu.uniquindio.servly.model.enums.AuthProvider;
import co.edu.uniquindio.servly.model.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false, length = 150)
    private String email;

    // Null si el usuario es OAuth2
    @Column(length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // ── OAuth2 ─────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    @Column(length = 255)
    private String providerId;

    // ── 2FA ────────────────────────────

    @Column(nullable = false)
    @Builder.Default
    private boolean twoFactorEnabled = false;

    // ── Estado de cuenta ───────────────

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean accountNonExpired = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean accountNonLocked = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean credentialsNonExpired = true;

    // ── Primer Login y Cambio de Password ───────────────

    /**
     * Indica si el usuario debe cambiar su contraseña en el primer login.
     * Usado cuando un admin crea un empleado con contraseña temporal.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean mustChangePassword = false;

    /**
     * Indica si el usuario ya completó su primer login exitosamente.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean firstLoginCompleted = false;

    /**
     * Fecha y hora en que se cambió la contraseña por última vez.
     */
    @Column
    private LocalDateTime passwordChangedAt;

    /**
     * Fecha y hora del primer login completado.
     */
    @Column
    private LocalDateTime firstLoginAt;

    /**
     * Versión de la contraseña. Se incrementa cada vez que el usuario cambia su contraseña.
     * Usado para invalidar tokens JWT antiguos al cambiar la contraseña.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer passwordVersion = 0;

    // ── Auditoría ──────────────────────

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ── Spring Security ────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }
}