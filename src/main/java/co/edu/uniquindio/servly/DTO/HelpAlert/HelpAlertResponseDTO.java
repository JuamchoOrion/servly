package co.edu.uniquindio.servly.DTO.HelpAlert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HelpAlertResponseDTO {
    private Long alertId;
    private Long orderId;
    private Integer tableNumber;
    private String status;
    private LocalDateTime createdAt;
}

