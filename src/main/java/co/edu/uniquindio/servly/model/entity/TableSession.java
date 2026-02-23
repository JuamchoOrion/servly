package co.edu.uniquindio.servly.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Sesión anónima de un cliente en una mesa del restaurante.
 *
 * Flujo:
 *  1. El cliente escanea el QR físico de la mesa
 *  2. El sistema crea un TableSession y genera un sessionToken JWT
 *  3. El cliente usa ese token para todas sus peticiones a /api/client/**
 *  4. Al facturar, el cajero/mesero cierra la sesión (active = false)
 *
 * Una mesa tiene máximo UNA sesión activa a la vez.
 * El sessionToken expira en 4 horas por defecto (configurable).
 */
@Entity
@Table(
        name = "table_sessions",
        indexes = {
                @Index(name = "idx_table_session_token",        columnList = "session_token"),
                @Index(name = "idx_table_session_table_active", columnList = "table_number, active")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private Integer tableNumber;

    @Column(name = "session_token", nullable = false, length = 512)
    private String sessionToken;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime openedAt;

    private LocalDateTime closedAt;
}
