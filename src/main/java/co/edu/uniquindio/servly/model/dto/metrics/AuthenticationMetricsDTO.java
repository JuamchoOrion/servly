package co.edu.uniquindio.servly.model.dto.metrics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO principal que agrupa todas las métricas de autenticación.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationMetricsDTO {

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    // Métrica 1: Tiempo promedio de autenticación
    private Double averageAuthenticationTimeMs;
    private String authenticationTimeStatus; // "OK" si < 2000ms, "WARNING" si >= 2000ms

    // Métrica 2: Tasa de accesos exitosos por rol
    private AuthMetricsDTO generalAuthMetrics;
    private AuthMetricsDTO adminAuthMetrics;
    private AuthMetricsDTO waiterAuthMetrics;
    private AuthMetricsDTO cashierAuthMetrics;
    private AuthMetricsDTO kitchenAuthMetrics;
    private AuthMetricsDTO storekeeperAuthMetrics;

    // Métrica 3: Tiempo promedio de recuperación de contraseña
    private Double averagePasswordRecoveryTimeMinutes;
    private String passwordRecoveryStatus; // "OK" si < 5 min, "WARNING" si >= 5 min

    // Métrica 4: Tiempo de verificación en dos pasos
    private TwoFactorMetricsDTO twoFactorMetrics;
    private String twoFactorStatus; // "OK" si < 60 seg, "WARNING" si >= 60 seg

    // Métrica 5: Tasa de expiración de códigos de verificación
    private PasswordRecoveryMetricsDTO passwordRecoveryMetrics;
    private String codeExpirationStatus; // "OK" si < 10%, "WARNING" si >= 10%

    // Métrica 6: Duración promedio de sesión activa
    private SessionMetricsDTO sessionMetrics;
    private String sessionDurationStatus;

    // Constructores de utilidad
    public static AuthenticationMetricsDTO withPeriod(LocalDateTime start, LocalDateTime end) {
        AuthenticationMetricsDTO dto = new AuthenticationMetricsDTO();
        dto.periodStart = start;
        dto.periodEnd = end;
        return dto;
    }

    // Métodos de evaluación de estado
    public void evaluateStatuses() {
        // Tiempo de autenticación (< 2000ms = OK)
        if (averageAuthenticationTimeMs != null) {
            authenticationTimeStatus = averageAuthenticationTimeMs < 2000 ? "OK" : "WARNING";
        }

        // Tiempo de recuperación de contraseña (< 5 min = OK)
        if (averagePasswordRecoveryTimeMinutes != null) {
            passwordRecoveryStatus = averagePasswordRecoveryTimeMinutes < 5 ? "OK" : "WARNING";
        }

        // Tiempo de 2FA (< 60 seg = OK)
        if (twoFactorMetrics != null && twoFactorMetrics.getAverageVerificationTimeSeconds() != null) {
            twoFactorStatus = twoFactorMetrics.getAverageVerificationTimeSeconds() < 60 ? "OK" : "WARNING";
        }

        // Tasa de expiración de códigos (< 10% = OK)
        if (passwordRecoveryMetrics != null && passwordRecoveryMetrics.getCodeExpirationRate() != null) {
            codeExpirationStatus = passwordRecoveryMetrics.getCodeExpirationRate() < 10 ? "OK" : "WARNING";
        }

        // Tasa de éxito general (> 95% = OK)
        if (generalAuthMetrics != null && generalAuthMetrics.getSuccessRate() != null) {
            if (generalAuthMetrics.getSuccessRate() < 95) {
                if (authenticationTimeStatus == null || !"WARNING".equals(authenticationTimeStatus)) {
                    authenticationTimeStatus = "LOW_SUCCESS_RATE";
                }
            }
        }
    }
}
