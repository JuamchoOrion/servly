package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Inventory.StockBatchCreateRequest;
import co.edu.uniquindio.servly.DTO.Inventory.StockBatchDTO;
import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.model.entity.*;
import co.edu.uniquindio.servly.repository.ItemStockRepository;
import co.edu.uniquindio.servly.repository.StockBatchRepository;
import co.edu.uniquindio.servly.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockBatchService Tests")
class StockBatchServiceTest {

    @Mock StockBatchRepository stockBatchRepository;
    @Mock ItemStockRepository itemStockRepository;
    @Mock SupplierRepository supplierRepository;

    @InjectMocks StockBatchService stockBatchService;

    private ItemStock itemStock;
    private Supplier supplier;
    private StockBatch batch;

    @BeforeEach
    void setUp() {
        Item item = new Item();
        item.setId(1L);
        item.setName("Arroz");
        item.setExpirationDays(30);

        supplier = new Supplier();
        supplier.setId(1L);
        supplier.setName("Proveedor ABC");

        itemStock = new ItemStock();
        itemStock.setId(1L);
        itemStock.setQuantity(100);
        itemStock.setItem(item);

        batch = StockBatch.builder()
                .id(1L)
                .batchNumber("LOTE-001")
                .quantity(50)
                .supplier(supplier)
                .expiryDate(LocalDate.now().plusDays(30))
                .createdDate(LocalDate.now())
                .status("VIGENTE")
                .itemStock(itemStock)
                .build();
    }

    // ── createBatch ───────────────────────────────────────────────────────

    @Test
    @DisplayName("createBatch - crea lote con fecha proporcionada")
    void createBatch_withExpiryDate_success() {
        StockBatchCreateRequest request = StockBatchCreateRequest.builder()
                .itemStockId(1L).quantity(50).supplierId(1L)
                .batchNumber("LOTE-001").expiryDate(LocalDate.now().plusDays(30)).build();

        when(itemStockRepository.findById(1L)).thenReturn(Optional.of(itemStock));
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(stockBatchRepository.save(any())).thenReturn(batch);
        when(itemStockRepository.save(any())).thenReturn(itemStock);

        StockBatchDTO result = stockBatchService.createBatch(request);

        assertThat(result.getBatchNumber()).isEqualTo("LOTE-001");
        assertThat(result.getQuantity()).isEqualTo(50);
        assertThat(result.getSupplierName()).isEqualTo("Proveedor ABC");
        verify(itemStockRepository).save(itemStock);
        assertThat(itemStock.getQuantity()).isEqualTo(150); // 100 + 50
    }

    @Test
    @DisplayName("createBatch - sin expiryDate calcula automáticamente desde expirationDays")
    void createBatch_withoutExpiryDate_calculatesAutomatically() {
        StockBatchCreateRequest request = StockBatchCreateRequest.builder()
                .itemStockId(1L).quantity(20).supplierId(null)
                .batchNumber("LOTE-AUTO").expiryDate(null).build();

        StockBatch savedBatch = StockBatch.builder()
                .id(2L).batchNumber("LOTE-AUTO").quantity(20)
                .expiryDate(LocalDate.now().plusDays(30))
                .createdDate(LocalDate.now()).status("VIGENTE")
                .itemStock(itemStock).build();

        when(itemStockRepository.findById(1L)).thenReturn(Optional.of(itemStock));
        when(stockBatchRepository.save(any())).thenReturn(savedBatch);
        when(itemStockRepository.save(any())).thenReturn(itemStock);

        StockBatchDTO result = stockBatchService.createBatch(request);

        assertThat(result.getExpiryDate()).isEqualTo(LocalDate.now().plusDays(30));
    }

    @Test
    @DisplayName("createBatch - sin expiryDate y sin expirationDays lanza AuthException")
    void createBatch_noExpiryAndNoExpirationDays_throws() {
        itemStock.getItem().setExpirationDays(null);

        StockBatchCreateRequest request = StockBatchCreateRequest.builder()
                .itemStockId(1L).quantity(10).expiryDate(null).build();

        when(itemStockRepository.findById(1L)).thenReturn(Optional.of(itemStock));

        assertThatThrownBy(() -> stockBatchService.createBatch(request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("expirationDays");
    }

    @Test
    @DisplayName("createBatch - cantidad <= 0 lanza AuthException")
    void createBatch_invalidQuantity_throws() {
        StockBatchCreateRequest request = StockBatchCreateRequest.builder()
                .itemStockId(1L).quantity(0).expiryDate(LocalDate.now().plusDays(10)).build();

        when(itemStockRepository.findById(1L)).thenReturn(Optional.of(itemStock));

        assertThatThrownBy(() -> stockBatchService.createBatch(request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("cantidad debe ser mayor a 0");
    }

    @Test
    @DisplayName("createBatch - ItemStock no encontrado lanza AuthException")
    void createBatch_itemStockNotFound_throws() {
        when(itemStockRepository.findById(99L)).thenReturn(Optional.empty());

        StockBatchCreateRequest request = StockBatchCreateRequest.builder()
                .itemStockId(99L).quantity(10).build();

        assertThatThrownBy(() -> stockBatchService.createBatch(request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("ItemStock no encontrado");
    }

    @Test
    @DisplayName("createBatch - supplier no encontrado lanza AuthException")
    void createBatch_supplierNotFound_throws() {
        StockBatchCreateRequest request = StockBatchCreateRequest.builder()
                .itemStockId(1L).quantity(10).supplierId(99L)
                .expiryDate(LocalDate.now().plusDays(10)).build();

        when(itemStockRepository.findById(1L)).thenReturn(Optional.of(itemStock));
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockBatchService.createBatch(request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Proveedor no encontrado");
    }

    // ── getBatchesByItemStock ─────────────────────────────────────────────

    @Test
    @DisplayName("getBatchesByItemStock - retorna lista ordenada por expiryDate")
    void getBatchesByItemStock_success() {
        when(itemStockRepository.findById(1L)).thenReturn(Optional.of(itemStock));
        when(stockBatchRepository.findByItemStockOrderByExpiryDateAsc(itemStock))
                .thenReturn(List.of(batch));

        List<StockBatchDTO> result = stockBatchService.getBatchesByItemStock(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBatchNumber()).isEqualTo("LOTE-001");
    }

    @Test
    @DisplayName("getBatchesByItemStock - ItemStock no encontrado lanza AuthException")
    void getBatchesByItemStock_notFound_throws() {
        when(itemStockRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockBatchService.getBatchesByItemStock(99L))
                .isInstanceOf(AuthException.class);
    }

    // ── getNextToExpireBatch ──────────────────────────────────────────────

    @Test
    @DisplayName("getNextToExpireBatch - retorna el lote más próximo")
    void getNextToExpireBatch_success() {
        when(itemStockRepository.findById(1L)).thenReturn(Optional.of(itemStock));
        when(stockBatchRepository.findActiveBatchesByItemStock(itemStock)).thenReturn(List.of(batch));

        StockBatchDTO result = stockBatchService.getNextToExpireBatch(1L);

        assertThat(result.getBatchNumber()).isEqualTo("LOTE-001");
    }

    @Test
    @DisplayName("getNextToExpireBatch - sin lotes vigentes lanza AuthException")
    void getNextToExpireBatch_noActiveBatches_throws() {
        when(itemStockRepository.findById(1L)).thenReturn(Optional.of(itemStock));
        when(stockBatchRepository.findActiveBatchesByItemStock(itemStock)).thenReturn(List.of());

        assertThatThrownBy(() -> stockBatchService.getNextToExpireBatch(1L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("No hay lotes vigentes");
    }

    // ── getExpiredBatches ─────────────────────────────────────────────────

    @Test
    @DisplayName("getExpiredBatches - retorna lotes expirados")
    void getExpiredBatches_success() {
        StockBatch expired = StockBatch.builder()
                .id(2L).batchNumber("LOTE-OLD").quantity(0)
                .expiryDate(LocalDate.now().minusDays(5))
                .createdDate(LocalDate.now().minusDays(40))
                .status("EXPIRADO").itemStock(itemStock).build();

        when(stockBatchRepository.findExpiredBatches()).thenReturn(List.of(expired));

        List<StockBatchDTO> result = stockBatchService.getExpiredBatches();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("AGOTADO"); // quantity=0 → AGOTADO dinámico
    }

    // ── getBatchesCloseTExpiry ────────────────────────────────────────────

    @Test
    @DisplayName("getBatchesCloseToExpiry - retorna lotes próximos a vencer")
    void getBatchesCloseToExpiry_success() {
        StockBatch closing = StockBatch.builder()
                .id(3L).batchNumber("LOTE-CLOSE").quantity(10)
                .expiryDate(LocalDate.now().plusDays(3))
                .createdDate(LocalDate.now()).status("VIGENTE").itemStock(itemStock).build();

        when(stockBatchRepository.findBatchesCloseTExpiry()).thenReturn(List.of(closing));

        List<StockBatchDTO> result = stockBatchService.getBatchesCloseTExpiry();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("PROXIMO_A_EXPIRAR");
    }

    // ── decreaseQuantity ──────────────────────────────────────────────────

    @Test
    @DisplayName("decreaseQuantity - FIFO: consume lote más próximo a expirar")
    void decreaseQuantity_fifo_success() {
        when(itemStockRepository.findById(1L)).thenReturn(Optional.of(itemStock));
        when(stockBatchRepository.findActiveBatchesByItemStock(itemStock)).thenReturn(List.of(batch));
        when(stockBatchRepository.saveAll(anyList())).thenReturn(List.of(batch));
        when(itemStockRepository.save(any())).thenReturn(itemStock);

        StockBatchDTO result = stockBatchService.decreaseQuantity(1L, 20);

        assertThat(batch.getQuantity()).isEqualTo(30); // 50 - 20
        assertThat(itemStock.getQuantity()).isEqualTo(80); // 100 - 20
    }

    @Test
    @DisplayName("decreaseQuantity - agota lote completamente y lo marca AGOTADO")
    void decreaseQuantity_exhaustsBatch() {
        when(itemStockRepository.findById(1L)).thenReturn(Optional.of(itemStock));
        when(stockBatchRepository.findActiveBatchesByItemStock(itemStock)).thenReturn(List.of(batch));
        when(stockBatchRepository.saveAll(anyList())).thenReturn(List.of(batch));
        when(itemStockRepository.save(any())).thenReturn(itemStock);

        stockBatchService.decreaseQuantity(1L, 50);

        assertThat(batch.getQuantity()).isEqualTo(0);
        assertThat(batch.getStatus()).isEqualTo("AGOTADO");
    }

    @Test
    @DisplayName("decreaseQuantity - sin lotes vigentes lanza AuthException")
    void decreaseQuantity_noActiveBatches_throws() {
        when(itemStockRepository.findById(1L)).thenReturn(Optional.of(itemStock));
        when(stockBatchRepository.findActiveBatchesByItemStock(itemStock)).thenReturn(List.of());

        assertThatThrownBy(() -> stockBatchService.decreaseQuantity(1L, 10))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("No hay lotes vigentes");
    }

    @Test
    @DisplayName("decreaseQuantity - cantidad mayor al stock disponible lanza AuthException")
    void decreaseQuantity_insufficientStock_throws() {
        when(itemStockRepository.findById(1L)).thenReturn(Optional.of(itemStock));
        when(stockBatchRepository.findActiveBatchesByItemStock(itemStock)).thenReturn(List.of(batch));

        assertThatThrownBy(() -> stockBatchService.decreaseQuantity(1L, 999))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("No hay suficiente cantidad");
    }

    // ── updateBatchStatus ─────────────────────────────────────────────────

    @Test
    @DisplayName("updateBatchStatus - actualiza estado correctamente")
    void updateBatchStatus_success() {
        StockBatch updated = StockBatch.builder()
                .id(1L).batchNumber("LOTE-001").quantity(50)
                .expiryDate(LocalDate.now().plusDays(30))
                .status("AGOTADO").itemStock(itemStock).build();

        when(stockBatchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(stockBatchRepository.save(any())).thenReturn(updated);

        StockBatchDTO result = stockBatchService.updateBatchStatus(1L, "AGOTADO");

        assertThat(batch.getStatus()).isEqualTo("AGOTADO");
    }

    @Test
    @DisplayName("updateBatchStatus - lote no encontrado lanza AuthException")
    void updateBatchStatus_notFound_throws() {
        when(stockBatchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockBatchService.updateBatchStatus(99L, "AGOTADO"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Lote no encontrado");
    }

    // ── deleteBatch ───────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteBatch - soft delete marca deletedAt y actualiza stock")
    void deleteBatch_success() {
        batch.setDeletedAt(null);

        when(stockBatchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(itemStockRepository.save(any())).thenReturn(itemStock);
        when(stockBatchRepository.save(any())).thenReturn(batch);

        stockBatchService.deleteBatch(1L);

        assertThat(batch.getDeletedAt()).isNotNull();
        assertThat(batch.getStatus()).isEqualTo("ELIMINADO");
        assertThat(itemStock.getQuantity()).isEqualTo(50); // 100 - 50
        verify(stockBatchRepository).save(batch);
    }

    @Test
    @DisplayName("deleteBatch - lote ya eliminado lanza AuthException")
    void deleteBatch_alreadyDeleted_throws() {
        batch.setDeletedAt(java.time.LocalDateTime.now().minusDays(1));

        when(stockBatchRepository.findById(1L)).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> stockBatchService.deleteBatch(1L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("ya ha sido eliminado");
    }

    @Test
    @DisplayName("deleteBatch - lote no encontrado lanza AuthException")
    void deleteBatch_notFound_throws() {
        when(stockBatchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockBatchService.deleteBatch(99L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Lote no encontrado");
    }
}