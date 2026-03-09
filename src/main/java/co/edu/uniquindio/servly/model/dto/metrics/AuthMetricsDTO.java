package co.edu.uniquindio.servly.model.dto.metrics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para métricas de autenticación.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthMetricsDTO {
    private Long totalAttempts;
    private Long successfulAttempts;
    private Double averageDurationMs;

    public Double getSuccessRate() {
        if (totalAttempts == null || totalAttempts == 0) return 0.0;
        return (successfulAttempts * 100.0) / totalAttempts;
    }
}
