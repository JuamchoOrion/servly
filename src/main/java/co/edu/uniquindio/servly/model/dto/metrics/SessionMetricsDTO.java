package co.edu.uniquindio.servly.model.dto.metrics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para métricas de sesión.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessionMetricsDTO {
    private Long totalSessions;
    private Double averageDurationSeconds;
    private Double averageDurationMinutes;

    public SessionMetricsDTO(Long totalSessions, Double averageDurationSeconds) {
        this.totalSessions = totalSessions;
        this.averageDurationSeconds = averageDurationSeconds;
        this.averageDurationMinutes = averageDurationSeconds != null ? averageDurationSeconds / 60.0 : 0.0;
    }
}
