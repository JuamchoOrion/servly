package co.edu.uniquindio.servly.controller;

import co.edu.uniquindio.servly.DTO.Inventory.ItemStockDTO;
import co.edu.uniquindio.servly.DTO.Inventory.PaginatedInventoryResponse;
import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * GET /api/inventory
     * Obtiene todos los items con su stock
     * Acceso: ADMIN, STOREKEEPER
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<List<ItemStockDTO>> getInventory() {
        return ResponseEntity.ok(inventoryService.getInventory());
    }

    /**
     * GET /api/inventory/paginated
     * Obtiene los items con su stock de manera paginada
     * Acceso: ADMIN, STOREKEEPER
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<PaginatedInventoryResponse> getInventoryPaginated(
            @PageableDefault(size = 10, page = 0, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(inventoryService.getInventoryPaginated(pageable));
    }

    /**
     * PUT /api/inventory/?/increase
     * Añade stock de un item
     * Acceso: ADMIN, STOREKEEPER
     */
    @PutMapping("/{itemStockId}/increase")
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<MessageResponse> increaseStock(
            @PathVariable Long itemStockId,
            @RequestParam Integer quantity) {

        inventoryService.increaseStock(itemStockId, quantity);

        return ResponseEntity.ok(
                new MessageResponse("Stock aumentado correctamente")
        );
    }

    /**
     * PUT /api/inventory/?/decrease
     * Elimina stock de un item
     * Acceso: ADMIN, STOREKEEPER
     */
    @PutMapping("/{itemStockId}/decrease")
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<MessageResponse> decreaseStock(
            @PathVariable Long itemStockId,
            @RequestParam Integer quantity) {

        inventoryService.decreaseStock(itemStockId, quantity);

        return ResponseEntity.ok(
                new MessageResponse("Stock reducido correctamente")
        );
    }
}