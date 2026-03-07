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
    private Long id;
    private Integer quantity;
    private String supplier;
    private Long itemId;
    private Long inventoryId;
}
