package co.edu.uniquindio.servly.DTO.Inventory;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO para actualizar un Item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemUpdateRequest {
    @NotNull
    private Long id;

    @NotNull
    @Size(min = 1, max = 255)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotNull
    @Size(min = 1, max = 50)
    private String unitOfMeasurement;

    @Min(0)
    private Integer expirationDays;

    @Size(max = 255)
    private String category;
}
