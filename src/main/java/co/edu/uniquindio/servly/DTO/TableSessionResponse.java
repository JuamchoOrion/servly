package co.edu.uniquindio.servly.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Respuesta al escanear el QR de la mesa.
 * El frontend guarda el sessionToken y lo envía como Bearer en cada petición.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableSessionResponse {

    private String        sessionToken;
    private Integer       tableNumber;
    private String        sessionId;
    private LocalDateTime expiresAt;

    @Builder.Default
    private String tokenType = "Bearer";
}