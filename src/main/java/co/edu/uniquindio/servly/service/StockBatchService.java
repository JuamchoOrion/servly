package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Inventory.StockBatchCreateRequest;
import co.edu.uniquindio.servly.DTO.Inventory.StockBatchDTO;
import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.model.entity.Item;
import co.edu.uniquindio.servly.model.entity.ItemStock;
import co.edu.uniquindio.servly.model.entity.StockBatch;
import co.edu.uniquindio.servly.model.entity.Supplier;
import co.edu.uniquindio.servly.repository.ItemStockRepository;
import co.edu.uniquindio.servly.repository.StockBatchRepository;
import co.edu.uniquindio.servly.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StockBatchService {

    private final StockBatchRepository stockBatchRepository;
    private final ItemStockRepository itemStockRepository;
    private final SupplierRepository supplierRepository;

    /**
     * Crea un nuevo lote de stock para un ItemStock.
     * Permite múltiples entradas del mismo item con diferentes vencimientos y proveedores.
     *
     * La fecha de expiración se calcula automáticamente como:
     * expiryDate = hoy + Item.expirationDays (si no se proporciona expiryDate)
     *
     * @param request contiene: itemStockId, quantity, supplierId, batchNumber, expiryDate (opcional)
     * @return StockBatchDTO con los datos del lote creado
     */
    public StockBatchDTO createBatch(StockBatchCreateRequest request) {
        log.info("Creando nuevo lote para ItemStock ID: {}", request.getItemStockId());

        // Validar ItemStock
        ItemStock itemStock = itemStockRepository.findById(request.getItemStockId())
                .orElseThrow(() -> new AuthException("ItemStock no encontrado"));

        // Validar cantidad
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new AuthException("La cantidad debe ser mayor a 0");
        }

        // Validar supplier si se proporciona
        Supplier supplier = null;
        if (request.getSupplierId() != null) {
            supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new AuthException("Proveedor no encontrado"));
        }

        // Calcular fecha de expiración automáticamente si no se proporciona
        LocalDate expiryDate = request.getExpiryDate();
        if (expiryDate == null) {
            // Obtener los días de expiración del item
            Item item = itemStock.getItem();
            Integer expirationDays = item.getExpirationDays();

            if (expirationDays != null && expirationDays > 0) {
                // Calcular: fecha actual + días de expiración
                expiryDate = LocalDate.now().plusDays(expirationDays);
                log.info("Fecha de expiración calculada automáticamente: {} ({} días desde hoy)",
                         expiryDate, expirationDays);
            } else {
                throw new AuthException("El item no tiene configurado expirationDays y no se proporcionó expiryDate");
            }
        }

        // Crear nuevo lote
        StockBatch batch = StockBatch.builder()
                .itemStock(itemStock)
                .batchNumber(request.getBatchNumber())
                .quantity(request.getQuantity())
                .supplier(supplier)
                .expiryDate(expiryDate)
                .createdDate(LocalDate.now())
                .status("VIGENTE")
                .build();

        StockBatch savedBatch = stockBatchRepository.save(batch);

        // Actualizar cantidad total en ItemStock
        itemStock.setQuantity(itemStock.getQuantity() + request.getQuantity());
        itemStockRepository.save(itemStock);

        log.info("Lote creado exitosamente con ID: {} - Vence: {}", savedBatch.getId(), expiryDate);

        return convertToDTO(savedBatch);
    }

    /**
     * Obtiene todos los lotes de un ItemStock, ordenados por fecha de vencimiento (FIFO).
     */
    @Transactional(readOnly = true)
    public List<StockBatchDTO> getBatchesByItemStock(Long itemStockId) {
        log.info("Obteniendo lotes para ItemStock ID: {}", itemStockId);

        ItemStock itemStock = itemStockRepository.findById(itemStockId)
                .orElseThrow(() -> new AuthException("ItemStock no encontrado"));

        return stockBatchRepository.findByItemStockOrderByExpiryDateAsc(itemStock).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el lote próximo a expirar de un ItemStock (para FIFO).
     */
    @Transactional(readOnly = true)
    public StockBatchDTO getNextToExpireBatch(Long itemStockId) {
        log.info("Obteniendo lote próximo a expirar para ItemStock ID: {}", itemStockId);

        ItemStock itemStock = itemStockRepository.findById(itemStockId)
                .orElseThrow(() -> new AuthException("ItemStock no encontrado"));

        StockBatch batch = stockBatchRepository.findActiveBatchesByItemStock(itemStock)
                .stream()
                .findFirst()
                .orElseThrow(() -> new AuthException("No hay lotes vigentes para este ItemStock"));

        return convertToDTO(batch);
    }

    /**
     * Obtiene todos los lotes expirados.
     */
    @Transactional(readOnly = true)
    public List<StockBatchDTO> getExpiredBatches() {
        log.info("Obteniendo lotes expirados");

        return stockBatchRepository.findExpiredBatches().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene todos los lotes próximos a expirar (menos de 7 días).
     */
    @Transactional(readOnly = true)
    public List<StockBatchDTO> getBatchesCloseTExpiry() {
        log.info("Obteniendo lotes próximos a expirar");

        return stockBatchRepository.findBatchesCloseTExpiry().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Disminuye la cantidad de un lote (por consumo/venta).
     * Usa FIFO: disminuye el lote más próximo a expirar primero.
     */
    public StockBatchDTO decreaseQuantity(Long itemStockId, Integer quantity) {
        log.info("Disminuyendo cantidad para ItemStock ID: {}", itemStockId);

        ItemStock itemStock = itemStockRepository.findById(itemStockId)
                .orElseThrow(() -> new AuthException("ItemStock no encontrado"));

        List<StockBatch> activeBatches = stockBatchRepository.findActiveBatchesByItemStock(itemStock);

        if (activeBatches.isEmpty()) {
            throw new AuthException("No hay lotes vigentes para disminuir cantidad");
        }

        int remainingQuantity = quantity;
        StockBatch lastModifiedBatch = null;

        // Usar FIFO: disminuir primero del lote más próximo a expirar
        for (StockBatch batch : activeBatches) {
            if (remainingQuantity <= 0) break;

            if (batch.getQuantity() <= remainingQuantity) {
                remainingQuantity -= batch.getQuantity();
                batch.setQuantity(0);
                batch.setStatus("AGOTADO");
                lastModifiedBatch = batch;
            } else {
                batch.setQuantity(batch.getQuantity() - remainingQuantity);
                batch.updateStatus();
                remainingQuantity = 0;
                lastModifiedBatch = batch;
            }
        }

        if (remainingQuantity > 0) {
            throw new AuthException("No hay suficiente cantidad de stock. Faltaron: " + remainingQuantity);
        }

        // Actualizar cantidad total en ItemStock
        itemStock.setQuantity(itemStock.getQuantity() - quantity);
        itemStockRepository.save(itemStock);

        stockBatchRepository.saveAll(activeBatches);

        log.info("Cantidad disminuida exitosamente");
        return convertToDTO(lastModifiedBatch);
    }

    /**
     * Actualiza el estado de un lote.
     */
    public StockBatchDTO updateBatchStatus(Long batchId, String status) {
        log.info("Actualizando estado de lote ID: {}", batchId);

        StockBatch batch = stockBatchRepository.findById(batchId)
                .orElseThrow(() -> new AuthException("Lote no encontrado"));

        batch.setStatus(status);
        StockBatch updatedBatch = stockBatchRepository.save(batch);

        return convertToDTO(updatedBatch);
    }

    /**
     * Elimina (soft delete) un lote de stock.
     *
     * Soft Delete: No borra el registro de la BD, solo marca como eliminado con deletedAt.
     * Ventajas:
     * - Mantiene historial completo
     * - Permite recuperar datos si es necesario
     * - Preserva la integridad referencial
     * - Auditoría: saber cuándo se eliminó
     *
     * El lote se marca como eliminado pero no aparece en consultas normales.
     */
    public void deleteBatch(Long batchId) {
        log.info("Eliminando (soft delete) lote ID: {}", batchId);

        StockBatch batch = stockBatchRepository.findById(batchId)
                .orElseThrow(() -> new AuthException("Lote no encontrado"));

        // Validar que no esté ya eliminado
        if (batch.getDeletedAt() != null) {
            throw new AuthException("Este lote ya ha sido eliminado");
        }

        // Restar cantidad del ItemStock
        ItemStock itemStock = batch.getItemStock();
        itemStock.setQuantity(itemStock.getQuantity() - batch.getQuantity());
        itemStockRepository.save(itemStock);

        // 🆕 SOFT DELETE: Marcar como eliminado con timestamp
        batch.setDeletedAt(java.time.LocalDateTime.now());
        batch.setStatus("ELIMINADO"); // Opcional: cambiar status a ELIMINADO
        stockBatchRepository.save(batch);

        log.info("Lote eliminado correctamente (soft delete) en: {}", batch.getDeletedAt());
    }

    /**
     * Convierte StockBatch a StockBatchDTO
     *
     * IMPORTANTE: El status se calcula dinámicamente según daysUntilExpiry:
     * - daysUntilExpiry < 0       → EXPIRADO
     * - 0 <= daysUntilExpiry <= 7 → PROXIMO_A_EXPIRAR
     * - daysUntilExpiry > 7       → VIGENTE
     * - quantity == 0             → AGOTADO
     */
    private StockBatchDTO convertToDTO(StockBatch batch) {
        Integer daysUntilExpiry = batch.getDaysUntilExpiry();
        String dynamicStatus;

        // Calcular status dinámicamente
        if (batch.getQuantity() == 0) {
            dynamicStatus = "AGOTADO";
        } else if (daysUntilExpiry < 0) {
            dynamicStatus = "EXPIRADO";
        } else if (daysUntilExpiry >= 0 && daysUntilExpiry <= 7) {
            dynamicStatus = "PROXIMO_A_EXPIRAR";
        } else {
            dynamicStatus = "VIGENTE";
        }

        log.debug("Status calculado dinámicamente para lote {}: {} (daysUntilExpiry: {})",
                  batch.getBatchNumber(), dynamicStatus, daysUntilExpiry);

        return StockBatchDTO.builder()
                .id(batch.getId())
                .batchNumber(batch.getBatchNumber())
                .quantity(batch.getQuantity())
                .supplierName(batch.getSupplier() != null ? batch.getSupplier().getName() : "N/A")
                .createdDate(batch.getCreatedDate())
                .expiryDate(batch.getExpiryDate())
                .status(dynamicStatus)  // ← USA STATUS CALCULADO DINÁMICAMENTE
                .daysUntilExpiry(daysUntilExpiry)
                .build();
    }
}

