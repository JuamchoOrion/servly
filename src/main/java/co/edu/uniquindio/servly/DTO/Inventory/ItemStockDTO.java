package co.edu.uniquindio.servly.DTO.Inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representar stock de un item dentro de un inventario.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemStockDTO {
    private Long itemStockId;

    private String name;
    private String description;
    private String category;

    private Integer quantity;
    private String unitOfMeasurement;

    private String supplierName;

    private Integer expirationDays;

    private Integer stockPercent;
}
