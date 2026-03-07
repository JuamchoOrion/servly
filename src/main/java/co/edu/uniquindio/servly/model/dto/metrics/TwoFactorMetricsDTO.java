package co.edu.uniquindio.servly.model.dto.metrics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para métricas de verificación en dos pasos (2FA).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorMetricsDTO {
    private Long totalVerifications;
    private Double averageVerificationTimeMs;
    private Long failedVerifications;

    public Double getAverageVerificationTimeSeconds() {
        if (averageVerificationTimeMs == null) return 0.0;
        return averageVerificationTimeMs / 1000.0;
    }

    public Double getFailureRate() {
        if (totalVerifications == null || totalVerifications == 0) return 0.0;
        return (failedVerifications * 100.0) / totalVerifications;
    }
}
