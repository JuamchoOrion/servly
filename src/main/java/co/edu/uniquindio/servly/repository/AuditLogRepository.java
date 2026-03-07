package co.edu.uniquindio.servly.repository;

import co.edu.uniquindio.servly.model.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findByEmailOrderByCreatedAtDesc(String email);

    List<AuditLog> findByEventTypeAndCreatedAtBetween(String eventType, LocalDateTime start, LocalDateTime end);

    List<AuditLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM AuditLog a WHERE a.eventType = :eventType AND a.email = :email AND a.createdAt BETWEEN :start AND :end ORDER BY a.createdAt DESC")
    List<AuditLog> findByEventTypeAndEmailAndDateRange(
            @Param("eventType") String eventType,
            @Param("email") String email,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT new co.edu.uniquindio.servly.model.dto.metrics.AuthMetricsDTO(
            COUNT(a),
            SUM(CASE WHEN a.success = true THEN 1 ELSE 0 END),
            AVG(CAST(a.durationMs AS double))
        )
        FROM AuditLog a
        WHERE a.eventType IN ('LOGIN_REQUEST', 'LOGIN_SUCCESS', 'LOGIN_FAILED')
        AND a.createdAt BETWEEN :start AND :end
    """)
    co.edu.uniquindio.servly.model.dto.metrics.AuthMetricsDTO getLoginMetrics(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT new co.edu.uniquindio.servly.model.dto.metrics.AuthMetricsDTO(
            COUNT(a),
            SUM(CASE WHEN a.success = true THEN 1 ELSE 0 END),
            AVG(CAST(a.durationMs AS double))
        )
        FROM AuditLog a
        WHERE a.eventType IN ('LOGIN_REQUEST', 'LOGIN_SUCCESS', 'LOGIN_FAILED')
        AND a.role = :role
        AND a.createdAt BETWEEN :start AND :end
    """)
    co.edu.uniquindio.servly.model.dto.metrics.AuthMetricsDTO getLoginMetricsByRole(
            @Param("role") String role,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT new co.edu.uniquindio.servly.model.dto.metrics.TwoFactorMetricsDTO(
            COUNT(a),
            AVG(CAST(a.durationMs AS double)),
            (SELECT COUNT(a2) FROM AuditLog a2 WHERE a2.eventType = '2FA_VERIFICATION_FAILED' AND a2.createdAt BETWEEN :start AND :end)
        )
        FROM AuditLog a
        WHERE a.eventType = '2FA_VERIFICATION_REQUEST'
        AND a.createdAt BETWEEN :start AND :end
    """)
    co.edu.uniquindio.servly.model.dto.metrics.TwoFactorMetricsDTO getTwoFactorMetrics(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT new co.edu.uniquindio.servly.model.dto.metrics.PasswordRecoveryMetricsDTO(
            COUNT(a),
            (SELECT COUNT(a2) FROM AuditLog a2 WHERE a2.eventType = 'PASSWORD_RESET_SUCCESS' AND a2.createdAt BETWEEN :start AND :end),
            (SELECT COUNT(a3) FROM AuditLog a3 WHERE a3.eventType = 'CODE_EXPIRED' AND a3.createdAt BETWEEN :start AND :end)
        )
        FROM AuditLog a
        WHERE a.eventType = 'PASSWORD_RECOVERY_REQUEST'
        AND a.createdAt BETWEEN :start AND :end
    """)
    co.edu.uniquindio.servly.model.dto.metrics.PasswordRecoveryMetricsDTO getPasswordRecoveryMetrics(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT AVG(CAST(a.durationMs AS double))
        FROM AuditLog a
        WHERE a.eventType = 'SESSION_ENDED'
        AND a.sessionStartTime IS NOT NULL
        AND a.sessionEndTime IS NOT NULL
        AND a.createdAt BETWEEN :start AND :end
    """)
    Double getAverageSessionDuration(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query(value = """
        SELECT AVG(EXTRACT(EPOCH FROM (a2.created_at - a.created_at)) * 1000)
        FROM audit_logs a
        JOIN audit_logs a2 ON a.email = a2.email
        WHERE a.event_type = 'PASSWORD_RECOVERY_REQUEST'
        AND a2.event_type = 'LOGIN_SUCCESS'
        AND a2.created_at > a.created_at
        AND a.created_at BETWEEN :start AND :end
    """, nativeQuery = true)
    Long getAveragePasswordRecoveryTime(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    long deleteByCreatedAtBefore(LocalDateTime date);
}
