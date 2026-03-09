package co.edu.uniquindio.servly.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Registro de auditoría para eventos de autenticación.
 * Permite medir las métricas de calidad del proceso de autenticación.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(length = 20)
    private String role;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 255)
    private String errorMessage;

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 255)
    private String userAgent;

    @Column
    private Long durationMs;

    @Column
    private String sessionId;

    @Column
    private LocalDateTime sessionStartTime;

    @Column
    private LocalDateTime sessionEndTime;

    @Column
    private String metadata;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Tipos de eventos ─────────────────────────────────────

    public static final String EVENT_LOGIN_REQUEST = "LOGIN_REQUEST";
    public static final String EVENT_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String EVENT_LOGIN_FAILED = "LOGIN_FAILED";

    public static final String EVENT_2FA_CODE_SENT = "2FA_CODE_SENT";
    public static final String EVENT_2FA_VERIFICATION_REQUEST = "2FA_VERIFICATION_REQUEST";
    public static final String EVENT_2FA_VERIFICATION_SUCCESS = "2FA_VERIFICATION_SUCCESS";
    public static final String EVENT_2FA_VERIFICATION_FAILED = "2FA_VERIFICATION_FAILED";

    public static final String EVENT_PASSWORD_RECOVERY_REQUEST = "PASSWORD_RECOVERY_REQUEST";
    public static final String EVENT_PASSWORD_RECOVERY_CODE_SENT = "PASSWORD_RECOVERY_CODE_SENT";
    public static final String EVENT_PASSWORD_RESET_REQUEST = "PASSWORD_RESET_REQUEST";
    public static final String EVENT_PASSWORD_RESET_SUCCESS = "PASSWORD_RESET_SUCCESS";
    public static final String EVENT_PASSWORD_RESET_FAILED = "PASSWORD_RESET_FAILED";

    public static final String EVENT_LOGOUT = "LOGOUT";

    public static final String EVENT_OAUTH2_LOGIN_REQUEST = "OAUTH2_LOGIN_REQUEST";
    public static final String EVENT_OAUTH2_LOGIN_SUCCESS = "OAUTH2_LOGIN_SUCCESS";
    public static final String EVENT_OAUTH2_LOGIN_FAILED = "OAUTH2_LOGIN_FAILED";

    public static final String EVENT_TOKEN_REFRESH = "TOKEN_REFRESH";
    public static final String EVENT_TOKEN_REFRESH_FAILED = "TOKEN_REFRESH_FAILED";

    public static final String EVENT_SESSION_STARTED = "SESSION_STARTED";
    public static final String EVENT_SESSION_ENDED = "SESSION_ENDED";

    public static final String EVENT_CODE_EXPIRED = "CODE_EXPIRED";
}
