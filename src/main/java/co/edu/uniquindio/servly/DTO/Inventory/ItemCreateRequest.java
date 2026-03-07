package co.edu.uniquindio.servly.DTO.Inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO para crear un Item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemCreateRequest {
    private String name;
    private String description;
    private String unitOfMeasurement;
    private Integer expirationDays;
}
