package co.edu.uniquindio.servly.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad para almacenar tokens revocados (blacklist).
 * Se usa para invalidar refresh tokens antes de su expiración natural.
 */
@Entity
@Table(name = "revoked_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * El token JWT revocado.
     */
    @Column(nullable = false, length = 1000)
    private String token;

    /**
     * Email del usuario al que pertenece el token.
     */
    @Column(nullable = false, length = 150)
    private String userEmail;

    /**
     * Fecha de expiración del token (para poder limpiar registros antiguos).
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Fecha en que se revocó el token.
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime revokedAt;

    /**
     * Verifica si el token ha expirado.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
