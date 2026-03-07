package co.edu.uniquindio.servly.model.dto.metrics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para métricas de recuperación de contraseña.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordRecoveryMetricsDTO {
    private Long totalRecoveryRequests;
    private Long successfulResets;
    private Long expiredCodes;

    public Double getSuccessRate() {
        if (totalRecoveryRequests == null || totalRecoveryRequests == 0) return 0.0;
        return (successfulResets * 100.0) / totalRecoveryRequests;
    }

    public Double getCodeExpirationRate() {
        if (totalRecoveryRequests == null || totalRecoveryRequests == 0) return 0.0;
        return (expiredCodes * 100.0) / totalRecoveryRequests;
    }
}
