package co.edu.uniquindio.servly.controller;

import co.edu.uniquindio.servly.DTO.Inventory.StockBatchCreateRequest;
import co.edu.uniquindio.servly.DTO.Inventory.StockBatchDTO;
import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.service.StockBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock-batch")
@RequiredArgsConstructor
@Slf4j

public class ItemStockController {

    private final StockBatchService stockBatchService;

    /**
     * POST /api/stock-batch
     * Crea un nuevo lote de stock para un ItemStock.
     * Cada lote representa una entrada específica con cantidad, proveedor y fecha de vencimiento.
     *
     * Payload:
     * {
     *   "itemStockId": 1,
     *   "quantity": 50,
     *   "supplierId": 1,
     *   "batchNumber": "LOTE-2026-001",
     *   "expiryDate": "2026-06-08"
     * }
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<StockBatchDTO> createBatch(@RequestBody StockBatchCreateRequest request) {
        log.info("POST /api/stock-batch - Creando nuevo lote para ItemStock ID: {}", request.getItemStockId());
        StockBatchDTO createdBatch = stockBatchService.createBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBatch);
    }

    /**
     * GET /api/stock-batch/item-stock/{itemStockId}
     * Obtiene todos los lotes de un ItemStock específico, ordenados por fecha de vencimiento (FIFO).
     */
    @GetMapping("/item-stock/{itemStockId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<List<StockBatchDTO>> getBatchesByItemStock(@PathVariable Long itemStockId) {
        log.info("GET /api/stock-batch/item-stock/{} - Obteniendo lotes para ItemStock", itemStockId);
        List<StockBatchDTO> batches = stockBatchService.getBatchesByItemStock(itemStockId);
        return ResponseEntity.ok(batches);
    }

    /**
     * GET /api/stock-batch/item-stock/{itemStockId}/next-to-expire
     * Obtiene el lote próximo a expirar de un ItemStock (para FIFO).
     */
    @GetMapping("/item-stock/{itemStockId}/next-to-expire")
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<StockBatchDTO> getNextToExpireBatch(@PathVariable Long itemStockId) {
        log.info("GET /api/stock-batch/item-stock/{}/next-to-expire - Obteniendo lote próximo a expirar", itemStockId);
        StockBatchDTO batch = stockBatchService.getNextToExpireBatch(itemStockId);
        return ResponseEntity.ok(batch);
    }

    /**
     * GET /api/stock-batch/expired
     * Obtiene todos los lotes expirados.
     */
    @GetMapping("/expired")
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<List<StockBatchDTO>> getExpiredBatches() {
        log.info("GET /api/stock-batch/expired - Obteniendo lotes expirados");
        List<StockBatchDTO> batches = stockBatchService.getExpiredBatches();
        return ResponseEntity.ok(batches);
    }

    /**
     * GET /api/stock-batch/close-to-expire
     * Obtiene todos los lotes próximos a expirar (menos de 7 días).
     */
    @GetMapping("/close-to-expire")
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<List<StockBatchDTO>> getBatchesCloseTExpiry() {
        log.info("GET /api/stock-batch/close-to-expire - Obteniendo lotes próximos a expirar");
        List<StockBatchDTO> batches = stockBatchService.getBatchesCloseTExpiry();
        return ResponseEntity.ok(batches);
    }

    /**
     * PUT /api/stock-batch/item-stock/{itemStockId}/decrease
     * Disminuye la cantidad de stock usando FIFO (primero expira, primero se consume).
     */
    @PutMapping("/item-stock/{itemStockId}/decrease")
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<StockBatchDTO> decreaseQuantity(
            @PathVariable Long itemStockId,
            @RequestParam Integer quantity) {
        log.info("PUT /api/stock-batch/item-stock/{}/decrease - Disminuyendo cantidad: {}", itemStockId, quantity);
        StockBatchDTO updatedBatch = stockBatchService.decreaseQuantity(itemStockId, quantity);
        return ResponseEntity.ok(updatedBatch);
    }

    /**
     * PUT /api/stock-batch/{id}/status
     * Actualiza el estado de un lote (VIGENTE, PROXIMO_A_EXPIRAR, EXPIRADO, AGOTADO).
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<StockBatchDTO> updateBatchStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        log.info("PUT /api/stock-batch/{}/status - Actualizando estado a: {}", id, status);
        StockBatchDTO updatedBatch = stockBatchService.updateBatchStatus(id, status);
        return ResponseEntity.ok(updatedBatch);
    }

    /**
     * DELETE /api/stock-batch/{id}
     * Elimina un lote de stock.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<MessageResponse> deleteBatch(@PathVariable Long id) {
        log.info("DELETE /api/stock-batch/{} - Eliminando lote", id);
        stockBatchService.deleteBatch(id);
        return ResponseEntity.ok(new MessageResponse("Lote eliminado correctamente"));
    }
}

