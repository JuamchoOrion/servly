package co.edu.uniquindio.servly.DTO.Inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO para crear un registro de stock.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemStockCreateRequest {
    private Integer quantity;
    private Long supplierId;
    private Long itemId;
}
