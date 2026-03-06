package co.edu.uniquindio.servly.model.entity;

import co.edu.uniquindio.servly.model.enums.CodeType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Almacena códigos OTP temporales para:
 *  - Verificación en 2 pasos (TWO_FACTOR)
 *  - Recuperación de contraseña (PASSWORD_RESET)
 *
 * Seguridad:
 *  - Los códigos se almacenan hasheados con BCrypt
 *  - Un código solo puede usarse una vez (campo used)
 *  - Cada nuevo código invalida los anteriores del mismo tipo
 */
@Entity
@Table(
        name = "verification_codes",
        indexes = {
                @Index(name = "idx_verification_email_type", columnList = "email, type")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CodeType type;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpiredOrUsed() {
        return used || LocalDateTime.now().isAfter(expiresAt);
    }
}