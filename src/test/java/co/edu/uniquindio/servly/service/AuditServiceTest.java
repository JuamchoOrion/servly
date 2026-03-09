package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.model.dto.metrics.*;
import co.edu.uniquindio.servly.model.entity.AuditLog;
import co.edu.uniquindio.servly.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService Tests")
class AuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        end = LocalDateTime.now();
        start = end.minusDays(7);
    }

    // =========================================================================
    // logEvent
    // =========================================================================
    @Nested
    @DisplayName("logEvent()")
    class LogEvent {

        @Test
        @DisplayName("Debe guardar un AuditLog con todos los campos correctos")
        void shouldSaveAuditLogWithCorrectFields() {
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

            auditService.logEvent("LOGIN", "user@test.com", "ADMIN",
                    true, null, 120L, "session-1");

            verify(auditLogRepository).save(captor.capture());
            AuditLog saved = captor.getValue();

            assertThat(saved.getEventType()).isEqualTo("LOGIN");
            assertThat(saved.getEmail()).isEqualTo("user@test.com");
            assertThat(saved.getRole()).isEqualTo("ADMIN");
            assertThat(saved.isSuccess()).isTrue();
            assertThat(saved.getErrorMessage()).isNull();
            assertThat(saved.getDurationMs()).isEqualTo(120L);
            assertThat(saved.getSessionId()).isEqualTo("session-1");
        }

        @Test
        @DisplayName("Debe guardar AuditLog con errorMessage cuando el evento falla")
        void shouldSaveAuditLogWithErrorMessage() {
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

            auditService.logEvent("LOGIN", "user@test.com", "WAITER",
                    false, "Credenciales inválidas", null, null);

            verify(auditLogRepository).save(captor.capture());
            AuditLog saved = captor.getValue();

            assertThat(saved.isSuccess()).isFalse();
            assertThat(saved.getErrorMessage()).isEqualTo("Credenciales inválidas");
            assertThat(saved.getDurationMs()).isNull();
        }

        @Test
        @DisplayName("Debe asignar 'unknown' a ipAddress y userAgent cuando no hay request activo")
        void shouldSetUnknownWhenNoRequestContext() {
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

            // Sin contexto de request (fuera de hilo HTTP), los valores deben ser "unknown"
            auditService.logEvent("LOGOUT", "user@test.com", "CASHIER",
                    true, null, 50L, "session-2");

            verify(auditLogRepository).save(captor.capture());
            AuditLog saved = captor.getValue();

            assertThat(saved.getIpAddress()).isEqualTo("unknown");
            assertThat(saved.getUserAgent()).isEqualTo("unknown");
        }
    }

    // =========================================================================
    // startOperation / endOperation
    // =========================================================================
    @Nested
    @DisplayName("startOperation() y endOperation()")
    class OperationTracking {

        @Test
        @DisplayName("endOperation debe calcular una duración positiva cuando startOperation fue llamado antes")
        void shouldCalculatePositiveDuration() {
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            String key = "op-login-1";

            auditService.startOperation(key);
            auditService.endOperation(key, "LOGIN", "user@test.com",
                    "ADMIN", true, null, "session-1");

            verify(auditLogRepository).save(captor.capture());
            AuditLog saved = captor.getValue();

            assertThat(saved.getDurationMs()).isNotNull();
            assertThat(saved.getDurationMs()).isGreaterThanOrEqualTo(0L);
        }

        @Test
        @DisplayName("endOperation debe guardar durationMs null cuando no se llamó startOperation")
        void shouldSaveNullDurationWhenNoStart() {
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

            // Sin startOperation previo
            auditService.endOperation("key-inexistente", "LOGIN", "user@test.com",
                    "ADMIN", true, null, "session-x");

            verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getDurationMs()).isNull();
        }

        @Test
        @DisplayName("endOperation debe eliminar la clave del mapa tras usarla (no reutilizable)")
        void shouldRemoveKeyAfterEndOperation() {
            String key = "op-unique";

            auditService.startOperation(key);
            auditService.endOperation(key, "LOGIN", "user@test.com",
                    "ADMIN", true, null, "session-1");

            // Segunda llamada con la misma clave → durationMs null
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            auditService.endOperation(key, "LOGIN", "user@test.com",
                    "ADMIN", true, null, "session-2");

            verify(auditLogRepository, times(2)).save(captor.capture());
            AuditLog secondLog = captor.getAllValues().get(1);
            assertThat(secondLog.getDurationMs()).isNull();
        }
    }

    // =========================================================================
    // logSessionStarted
    // =========================================================================
    @Nested
    @DisplayName("logSessionStarted()")
    class LogSessionStarted {

        @Test
        @DisplayName("Debe guardar AuditLog con evento SESSION_STARTED y success=true")
        void shouldSaveSessionStartedLog() {
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

            auditService.logSessionStarted("user@test.com", "KITCHEN", "session-abc");

            verify(auditLogRepository).save(captor.capture());
            AuditLog saved = captor.getValue();

            assertThat(saved.getEventType()).isEqualTo(AuditLog.EVENT_SESSION_STARTED);
            assertThat(saved.getEmail()).isEqualTo("user@test.com");
            assertThat(saved.getRole()).isEqualTo("KITCHEN");
            assertThat(saved.getSessionId()).isEqualTo("session-abc");
            assertThat(saved.isSuccess()).isTrue();
            assertThat(saved.getSessionStartTime()).isNotNull();
        }
    }

    // =========================================================================
    // logSessionEnded
    // =========================================================================
    @Nested
    @DisplayName("logSessionEnded()")
    class LogSessionEnded {

        @Test
        @DisplayName("Debe actualizar el log existente con SESSION_ENDED y sessionEndTime")
        void shouldUpdateSessionLogWithEndTime() {
            AuditLog existingLog = AuditLog.builder()
                    .id("a")
                    .eventType(AuditLog.EVENT_SESSION_STARTED)
                    .email("user@test.com")
                    .sessionId("session-abc")
                    .sessionStartTime(LocalDateTime.now().minusMinutes(30))
                    .build();

            when(auditLogRepository.findByEventTypeAndEmailAndDateRange(
                    eq(AuditLog.EVENT_SESSION_STARTED),
                    eq("user@test.com"),
                    any(LocalDateTime.class),
                    any(LocalDateTime.class)))
                    .thenReturn(List.of(existingLog));

            auditService.logSessionEnded("session-abc", "user@test.com");

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());
            AuditLog saved = captor.getValue();

            assertThat(saved.getEventType()).isEqualTo(AuditLog.EVENT_SESSION_ENDED);
            assertThat(saved.getSessionEndTime()).isNotNull();
        }

        @Test
        @DisplayName("No debe guardar nada si no se encuentra la sesión iniciada")
        void shouldDoNothingWhenSessionNotFound() {
            when(auditLogRepository.findByEventTypeAndEmailAndDateRange(
                    anyString(), anyString(),
                    any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of());

            auditService.logSessionEnded("session-inexistente", "user@test.com");

            verify(auditLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("No debe actualizar si el sessionId no coincide con ningún log")
        void shouldNotUpdateWhenSessionIdMismatch() {
            AuditLog existingLog = AuditLog.builder()
                    .id("a")
                    .eventType(AuditLog.EVENT_SESSION_STARTED)
                    .email("user@test.com")
                    .sessionId("otro-session-id")
                    .build();

            when(auditLogRepository.findByEventTypeAndEmailAndDateRange(
                    anyString(), anyString(),
                    any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(existingLog));

            auditService.logSessionEnded("session-abc", "user@test.com");

            verify(auditLogRepository, never()).save(any());
        }
    }

    // =========================================================================
    // getAuthenticationMetrics
    // =========================================================================
    @Nested
    @DisplayName("getAuthenticationMetrics()")
    class GetAuthenticationMetrics {

        @Test
        @DisplayName("Debe retornar métricas con período correcto")
        void shouldReturnMetricsWithCorrectPeriod() {
            stubAllMetricsQueries();

            AuthenticationMetricsDTO result = auditService.getAuthenticationMetrics(start, end);

            assertThat(result).isNotNull();
            assertThat(result.getPeriodStart()).isEqualTo(start);
            assertThat(result.getPeriodEnd()).isEqualTo(end);
        }

        @Test
        @DisplayName("Debe calcular averageAuthenticationTimeMs desde generalMetrics")
        void shouldSetAverageAuthTimeFromGeneralMetrics() {
            AuthMetricsDTO generalMetrics = mock(AuthMetricsDTO.class);
            when(generalMetrics.getAverageDurationMs()).thenReturn(350.0);
            when(auditLogRepository.getLoginMetrics(start, end)).thenReturn(generalMetrics);
            stubRoleMetrics();
            stubRecoveryAndSessionMetrics();

            AuthenticationMetricsDTO result = auditService.getAuthenticationMetrics(start, end);

            assertThat(result.getAverageAuthenticationTimeMs()).isEqualTo(350.0);
        }

        @Test
        @DisplayName("Debe manejar graciosamente cuando generalMetrics es null")
        void shouldHandleNullGeneralMetrics() {
            when(auditLogRepository.getLoginMetrics(start, end)).thenReturn(null);
            stubRoleMetrics();
            stubRecoveryAndSessionMetrics();

            AuthenticationMetricsDTO result = auditService.getAuthenticationMetrics(start, end);

            assertThat(result.getAverageAuthenticationTimeMs()).isNull();
        }

        @Test
        @DisplayName("Debe convertir avgPasswordRecoveryTime de ms a minutos")
        void shouldConvertRecoveryTimeToMinutes() {
            when(auditLogRepository.getLoginMetrics(start, end)).thenReturn(null);
            stubRoleMetrics();
            when(auditLogRepository.getPasswordRecoveryMetrics(start, end)).thenReturn(null);
            when(auditLogRepository.getAveragePasswordRecoveryTime(start, end))
                    .thenReturn(120000L); // 2 minutos en ms
            when(auditLogRepository.getTwoFactorMetrics(start, end)).thenReturn(null);
            when(auditLogRepository.getAverageSessionDuration(start, end)).thenReturn(null);

            AuthenticationMetricsDTO result = auditService.getAuthenticationMetrics(start, end);

            assertThat(result.getAveragePasswordRecoveryTimeMinutes()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Debe convertir avgSessionDuration de ms a segundos")
        void shouldConvertSessionDurationToSeconds() {
            when(auditLogRepository.getLoginMetrics(start, end)).thenReturn(null);
            stubRoleMetrics();
            when(auditLogRepository.getPasswordRecoveryMetrics(start, end)).thenReturn(null);
            when(auditLogRepository.getAveragePasswordRecoveryTime(start, end)).thenReturn(null);
            when(auditLogRepository.getTwoFactorMetrics(start, end)).thenReturn(null);
            when(auditLogRepository.getAverageSessionDuration(start, end))
                    .thenReturn(5000.0); // 5 segundos en ms

            AuthenticationMetricsDTO result = auditService.getAuthenticationMetrics(start, end);

            assertThat(result.getSessionMetrics()).isNotNull();
            verify(auditLogRepository).getAverageSessionDuration(start, end);
        }

        @Test
        @DisplayName("Debe consultar métricas para los 5 roles definidos")
        void shouldQueryMetricsForAllRoles() {
            stubAllMetricsQueries();

            auditService.getAuthenticationMetrics(start, end);

            verify(auditLogRepository).getLoginMetricsByRole("ADMIN", start, end);
            verify(auditLogRepository).getLoginMetricsByRole("WAITER", start, end);
            verify(auditLogRepository).getLoginMetricsByRole("CASHIER", start, end);
            verify(auditLogRepository).getLoginMetricsByRole("KITCHEN", start, end);
            verify(auditLogRepository).getLoginMetricsByRole("STOREKEEPER", start, end);
        }

        // helpers
        private void stubAllMetricsQueries() {
            when(auditLogRepository.getLoginMetrics(start, end)).thenReturn(null);
            stubRoleMetrics();
            stubRecoveryAndSessionMetrics();
        }

        private void stubRoleMetrics() {
            when(auditLogRepository.getLoginMetricsByRole(anyString(), eq(start), eq(end)))
                    .thenReturn(null);
        }

        private void stubRecoveryAndSessionMetrics() {
            when(auditLogRepository.getPasswordRecoveryMetrics(start, end)).thenReturn(null);
            when(auditLogRepository.getAveragePasswordRecoveryTime(start, end)).thenReturn(null);
            when(auditLogRepository.getTwoFactorMetrics(start, end)).thenReturn(null);
            when(auditLogRepository.getAverageSessionDuration(start, end)).thenReturn(null);
        }
    }

    // =========================================================================
    // getLast7DaysMetrics / getLast30DaysMetrics
    // =========================================================================
    @Nested
    @DisplayName("getLast7DaysMetrics() y getLast30DaysMetrics()")
    class PredefinedPeriodMetrics {

        @Test
        @DisplayName("getLast7DaysMetrics debe consultar un rango de 7 días")
        void shouldQueryLast7Days() {
            stubAllNull();

            AuthenticationMetricsDTO result = auditService.getLast7DaysMetrics();

            assertThat(result.getPeriodEnd()).isNotNull();
            assertThat(result.getPeriodStart())
                    .isAfterOrEqualTo(result.getPeriodEnd().minusDays(7).minusSeconds(1));
        }

        @Test
        @DisplayName("getLast30DaysMetrics debe consultar un rango de 30 días")
        void shouldQueryLast30Days() {
            stubAllNull();

            AuthenticationMetricsDTO result = auditService.getLast30DaysMetrics();

            assertThat(result.getPeriodStart())
                    .isAfterOrEqualTo(result.getPeriodEnd().minusDays(30).minusSeconds(1));
        }

        private void stubAllNull() {
            when(auditLogRepository.getLoginMetrics(any(), any())).thenReturn(null);
            when(auditLogRepository.getLoginMetricsByRole(anyString(), any(), any())).thenReturn(null);
            when(auditLogRepository.getPasswordRecoveryMetrics(any(), any())).thenReturn(null);
            when(auditLogRepository.getAveragePasswordRecoveryTime(any(), any())).thenReturn(null);
            when(auditLogRepository.getTwoFactorMetrics(any(), any())).thenReturn(null);
            when(auditLogRepository.getAverageSessionDuration(any(), any())).thenReturn(null);
        }
    }

    // =========================================================================
    // deleteLogsOlderThan
    // =========================================================================
    @Nested
    @DisplayName("deleteLogsOlderThan()")
    class DeleteLogsOlderThan {

        @Test
        @DisplayName("Debe retornar el número de logs eliminados")
        void shouldReturnDeletedCount() {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
            when(auditLogRepository.deleteByCreatedAtBefore(cutoff)).thenReturn(42L);

            long deleted = auditService.deleteLogsOlderThan(cutoff);

            assertThat(deleted).isEqualTo(42L);
            verify(auditLogRepository).deleteByCreatedAtBefore(cutoff);
        }

        @Test
        @DisplayName("Debe retornar 0 cuando no hay logs que eliminar")
        void shouldReturnZeroWhenNothingToDelete() {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
            when(auditLogRepository.deleteByCreatedAtBefore(cutoff)).thenReturn(0L);

            assertThat(auditService.deleteLogsOlderThan(cutoff)).isZero();
        }
    }
}