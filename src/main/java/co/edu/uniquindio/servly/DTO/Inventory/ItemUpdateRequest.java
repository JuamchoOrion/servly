package co.edu.uniquindio.servly.DTO.Inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO para actualizar un Item.
 * Todos los campos son opcionales para permitir actualizaciones parciales.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemUpdateRequest {
    private String name;
    private String description;
    private String unitOfMeasurement;
    private Integer expirationDays;
    private String category;
    // Nuevo: permitir actualizar idealStock
    private Integer idealStock;
}
