package co.edu.uniquindio.servly.DTO.Inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO para actualizar un registro de stock.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemStockUpdateRequest {
    private Long id;
    private Integer quantity;
    private Long supplierId;
}
