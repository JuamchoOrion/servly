package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.model.dto.metrics.*;
import co.edu.uniquindio.servly.model.entity.AuditLog;
import co.edu.uniquindio.servly.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para registrar eventos de auditoría y calcular métricas de autenticación.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    // Mapa para trackear tiempos de inicio de operaciones
    private final Map<String, LocalDateTime> operationStartTimes = new HashMap<>();

    /**
     * Registra un evento de autenticación genérico.
     */
    @Transactional
    public void logEvent(String eventType, String email, String role, boolean success,
                         String errorMessage, Long durationMs, String sessionId) {
        HttpServletRequest request = getCurrentRequest();

        AuditLog auditLog = AuditLog.builder()
                .eventType(eventType)
                .email(email)
                .role(role)
                .success(success)
                .errorMessage(errorMessage)
                .ipAddress(getIpAddress(request))
                .userAgent(getUserAgent(request))
                .durationMs(durationMs)
                .sessionId(sessionId)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit log creado: {} - {} - success={}", eventType, email, success);
    }

    /**
     * Registra el inicio de una operación para luego calcular su duración.
     */
    public void startOperation(String operationKey) {
        operationStartTimes.put(operationKey, LocalDateTime.now());
    }

    /**
     * Registra el fin de una operación y guarda el log con la duración calculada.
     */
    @Transactional
    public void endOperation(String operationKey, String eventType, String email, String role,
                             boolean success, String errorMessage, String sessionId) {
        LocalDateTime startTime = operationStartTimes.remove(operationKey);
        Long durationMs = null;

        if (startTime != null) {
            durationMs = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
        }

        logEvent(eventType, email, role, success, errorMessage, durationMs, sessionId);
    }

    /**
     * Registra evento de sesión iniciada.
     */
    @Transactional
    public void logSessionStarted(String email, String role, String sessionId) {
        AuditLog auditLog = AuditLog.builder()
                .eventType(AuditLog.EVENT_SESSION_STARTED)
                .email(email)
                .role(role)
                .success(true)
                .sessionId(sessionId)
                .sessionStartTime(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }

    /**
     * Registra evento de sesión finalizada.
     */
    @Transactional
    public void logSessionEnded(String sessionId, String email) {
        LocalDateTime now = LocalDateTime.now();

        // Buscar la sesión iniciada y actualizarla
        auditLogRepository.findByEventTypeAndEmailAndDateRange(
                        AuditLog.EVENT_SESSION_STARTED, email,
                        now.minusHours(24), now)
                .stream()
                .filter(log -> sessionId.equals(log.getSessionId()))
                .findFirst()
                .ifPresent(startedLog -> {
                    startedLog.setEventType(AuditLog.EVENT_SESSION_ENDED);
                    startedLog.setSessionEndTime(now);
                    auditLogRepository.save(startedLog);
                });
    }

    /**
     * Calcula todas las métricas de autenticación para un período dado.
     */
    @Transactional(readOnly = true)
    public AuthenticationMetricsDTO getAuthenticationMetrics(LocalDateTime start, LocalDateTime end) {
        AuthenticationMetricsDTO metrics = AuthenticationMetricsDTO.withPeriod(start, end);

        // Métrica 1: Tiempo promedio de autenticación
        AuthMetricsDTO generalMetrics = auditLogRepository.getLoginMetrics(start, end);
        metrics.setGeneralAuthMetrics(generalMetrics);
        if (generalMetrics != null) {
            metrics.setAverageAuthenticationTimeMs(generalMetrics.getAverageDurationMs());
        }

        // Métrica 2: Tasa de accesos exitosos por rol
        metrics.setAdminAuthMetrics(auditLogRepository.getLoginMetricsByRole("ADMIN", start, end));
        metrics.setWaiterAuthMetrics(auditLogRepository.getLoginMetricsByRole("WAITER", start, end));
        metrics.setCashierAuthMetrics(auditLogRepository.getLoginMetricsByRole("CASHIER", start, end));
        metrics.setKitchenAuthMetrics(auditLogRepository.getLoginMetricsByRole("KITCHEN", start, end));
        metrics.setStorekeeperAuthMetrics(auditLogRepository.getLoginMetricsByRole("STOREKEEPER", start, end));

        // Métrica 3: Tiempo promedio de recuperación de contraseña
        PasswordRecoveryMetricsDTO recoveryMetrics = auditLogRepository.getPasswordRecoveryMetrics(start, end);
        metrics.setPasswordRecoveryMetrics(recoveryMetrics);
        Long avgRecoveryTimeMs = auditLogRepository.getAveragePasswordRecoveryTime(start, end);
        if (avgRecoveryTimeMs != null) {
            metrics.setAveragePasswordRecoveryTimeMinutes(avgRecoveryTimeMs / 60000.0);
        }

        // Métrica 4: Tiempo de verificación en dos pasos
        TwoFactorMetricsDTO twoFactorMetrics = auditLogRepository.getTwoFactorMetrics(start, end);
        metrics.setTwoFactorMetrics(twoFactorMetrics);

        // Métrica 5: Tasa de expiración de códigos (ya está en recoveryMetrics)

        // Métrica 6: Duración promedio de sesión activa (en milisegundos)
        Double avgSessionDurationMs = auditLogRepository.getAverageSessionDuration(start, end);
        // Convertir a segundos
        Double avgSessionDurationSeconds = avgSessionDurationMs != null ? avgSessionDurationMs / 1000.0 : null;
        metrics.setSessionMetrics(new SessionMetricsDTO(null, avgSessionDurationSeconds));

        // Evaluar estados
        metrics.evaluateStatuses();

        return metrics;
    }

    /**
     * Obtiene métricas para los últimos 7 días.
     */
    @Transactional(readOnly = true)
    public AuthenticationMetricsDTO getLast7DaysMetrics() {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(7);
        return getAuthenticationMetrics(start, end);
    }

    /**
     * Obtiene métricas para los últimos 30 días.
     */
    @Transactional(readOnly = true)
    public AuthenticationMetricsDTO getLast30DaysMetrics() {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(30);
        return getAuthenticationMetrics(start, end);
    }

    /**
     * Limpieza de logs antiguos.
     */
    @Transactional
    public long deleteLogsOlderThan(LocalDateTime date) {
        log.info("Eliminando logs de auditoría anteriores a: {}", date);
        return auditLogRepository.deleteByCreatedAtBefore(date);
    }

    // ── Utilidades ─────────────────────────────────────

    private HttpServletRequest getCurrentRequest() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String getIpAddress(HttpServletRequest request) {
        if (request == null) return "unknown";

        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String getUserAgent(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua.substring(0, Math.min(ua.length(), 255)) : "unknown";
    }
}
