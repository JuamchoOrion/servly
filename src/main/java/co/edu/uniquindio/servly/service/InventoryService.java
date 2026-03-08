package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Inventory.ItemStockDTO;
import co.edu.uniquindio.servly.DTO.Inventory.PaginatedInventoryResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InventoryService {
    /**
     * Retorna el inventario principal (primer inventario) en forma de lista de ItemStockDTO
     */
    List<ItemStockDTO> getInventory();

    PaginatedInventoryResponse getInventoryPaginated(Pageable pageable);


    /**
     * Aumenta la cantidad de stock de un itemStock
     */
    void increaseStock(Long itemStockId, Integer quantity);

    /**
     * Disminuye la cantidad de stock de un itemStock. Límite inferior: 0.
     */
    void decreaseStock(Long itemStockId, Integer quantity);
}
