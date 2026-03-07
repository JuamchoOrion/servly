package co.edu.uniquindio.servly.DTO.Inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO para crear un inventario con stock inicial opcional.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCreateRequest {
    // lista opcional de entradas iniciales de stock
    private List<ItemStockCreateRequest> initialStock;
}
