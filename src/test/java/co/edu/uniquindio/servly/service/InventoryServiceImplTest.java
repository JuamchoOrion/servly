package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Inventory.ItemStockDTO;
import co.edu.uniquindio.servly.DTO.Inventory.PaginatedInventoryResponse;
import co.edu.uniquindio.servly.exception.NotFoundException;
import co.edu.uniquindio.servly.model.entity.*;
import co.edu.uniquindio.servly.repository.InventoryRepository;
import co.edu.uniquindio.servly.repository.ItemStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryServiceImpl Tests")
class InventoryServiceImplTest {

    @Mock InventoryRepository inventoryRepository;
    @Mock ItemStockRepository itemStockRepository;

    @InjectMocks InventoryServiceImpl inventoryService;

    // ── Fixtures ──────────────────────────────────────────────────────────

    private Inventory inventory;
    private ItemStock itemStock;

    @BeforeEach
    void setUp() {
        // Categoría
        ItemCategory category = new ItemCategory();
        category.setId(1L);
        category.setName("Granos");

        // Item
        Item item = new Item();
        item.setId(1L);
        item.setName("Arroz Blanco");
        item.setDescription("Arroz de grano largo");
        item.setUnitOfMeasurement("kg");
        item.setExpirationDays(365);
        item.setIdealStock(50);
        item.setItemCategory(category);

        // Proveedor
        Supplier supplier = new Supplier();
        supplier.setId(1L);
        supplier.setName("Proveedor ABC");

        // Inventario
        inventory = new Inventory();
        inventory.setId(1L);

        // ItemStock
        itemStock = new ItemStock();
        itemStock.setId(1L);
        itemStock.setQuantity(100);
        itemStock.setItem(item);
        itemStock.setSupplier(supplier);
        itemStock.setInventory(inventory);
    }

    // ── getInventory ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getInventory - retorna lista de DTOs correctamente mapeados")
    void getInventory_success() {
        when(inventoryRepository.findAll()).thenReturn(List.of(inventory));
        when(itemStockRepository.findByInventoryId(1L)).thenReturn(List.of(itemStock));

        List<ItemStockDTO> result = inventoryService.getInventory();

        assertThat(result).hasSize(1);
        ItemStockDTO dto = result.get(0);
        assertThat(dto.getItemStockId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Arroz Blanco");
        assertThat(dto.getDescription()).isEqualTo("Arroz de grano largo");
        assertThat(dto.getCategory()).isEqualTo("Granos");
        assertThat(dto.getQuantity()).isEqualTo(100);
        assertThat(dto.getUnitOfMeasurement()).isEqualTo("kg");
        assertThat(dto.getSupplierName()).isEqualTo("Proveedor ABC");
        assertThat(dto.getExpirationDays()).isEqualTo(365);
        assertThat(dto.getIdealStock()).isEqualTo(50);
    }

    @Test
    @DisplayName("getInventory - sin inventario lanza NotFoundException")
    void getInventory_noInventory_throwsNotFound() {
        when(inventoryRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> inventoryService.getInventory())
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("No inventory found");
    }

    @Test
    @DisplayName("getInventory - itemStock sin item ni supplier mapea nulls sin error")
    void getInventory_itemStockWithNulls_mapsNullsSafely() {
        ItemStock emptyStock = new ItemStock();
        emptyStock.setId(2L);
        emptyStock.setQuantity(0);
        emptyStock.setItem(null);
        emptyStock.setSupplier(null);

        when(inventoryRepository.findAll()).thenReturn(List.of(inventory));
        when(itemStockRepository.findByInventoryId(1L)).thenReturn(List.of(emptyStock));

        List<ItemStockDTO> result = inventoryService.getInventory();

        assertThat(result).hasSize(1);
        ItemStockDTO dto = result.get(0);
        assertThat(dto.getName()).isNull();
        assertThat(dto.getCategory()).isNull();
        assertThat(dto.getSupplierName()).isNull();
    }

    @Test
    @DisplayName("getInventory - item sin categoría mapea category como null")
    void getInventory_itemWithNoCategory_categoryIsNull() {
        itemStock.getItem().setItemCategory(null);

        when(inventoryRepository.findAll()).thenReturn(List.of(inventory));
        when(itemStockRepository.findByInventoryId(1L)).thenReturn(List.of(itemStock));

        List<ItemStockDTO> result = inventoryService.getInventory();

        assertThat(result.get(0).getCategory()).isNull();
    }

    // ── getInventoryPaginated ─────────────────────────────────────────────

    @Test
    @DisplayName("getInventoryPaginated - retorna respuesta paginada correctamente")
    void getInventoryPaginated_success() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));
        Page<ItemStock> page = new PageImpl<>(List.of(itemStock), pageable, 1);

        when(inventoryRepository.findAll()).thenReturn(List.of(inventory));
        when(itemStockRepository.findByInventoryId(eq(1L), any(Pageable.class))).thenReturn(page);

        PaginatedInventoryResponse response = inventoryService.getInventoryPaginated(pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1L);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.getPageNumber()).isEqualTo(0);
        assertThat(response.getPageSize()).isEqualTo(10);
        assertThat(response.isLast()).isTrue();
        assertThat(response.getContent().get(0).getName()).isEqualTo("Arroz Blanco");
    }

    @Test
    @DisplayName("getInventoryPaginated - sin inventario lanza NotFoundException")
    void getInventoryPaginated_noInventory_throwsNotFound() {
        when(inventoryRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> inventoryService.getInventoryPaginated(Pageable.unpaged()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("No inventory found");
    }

    // ── increaseStock ─────────────────────────────────────────────────────

    @Test
    @DisplayName("increaseStock - suma cantidad correctamente")
    void increaseStock_success() {
        when(itemStockRepository.findById(1L)).thenReturn(Optional.of(itemStock));

        inventoryService.increaseStock(1L, 20);

        assertThat(itemStock.getQuantity()).isEqualTo(120);
        verify(itemStockRepository).save(itemStock);
    }

    @Test
    @DisplayName("increaseStock - quantity null no hace nada")
    void increaseStock_nullQuantity_doesNothing() {
        inventoryService.increaseStock(1L, null);
        verify(itemStockRepository, never()).findById(any());
        verify(itemStockRepository, never()).save(any());
    }

    @Test
    @DisplayName("increaseStock - quantity <= 0 no hace nada")
    void increaseStock_zeroOrNegative_doesNothing() {
        inventoryService.increaseStock(1L, 0);
        inventoryService.increaseStock(1L, -5);
        verify(itemStockRepository, never()).save(any());
    }

    @Test
    @DisplayName("increaseStock - itemStock no encontrado lanza NotFoundException")
    void increaseStock_notFound_throws() {
        when(itemStockRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.increaseStock(99L, 10))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("ItemStock not found with id: 99");
    }

    // ── decreaseStock ─────────────────────────────────────────────────────

    @Test
    @DisplayName("decreaseStock - resta cantidad correctamente")
    void decreaseStock_success() {
        when(itemStockRepository.findById(1L)).thenReturn(Optional.of(itemStock));

        inventoryService.decreaseStock(1L, 30);

        assertThat(itemStock.getQuantity()).isEqualTo(70);
        verify(itemStockRepository).save(itemStock);
    }

    @Test
    @DisplayName("decreaseStock - resultado negativo queda en 0")
    void decreaseStock_resultNegative_setsZero() {
        when(itemStockRepository.findById(1L)).thenReturn(Optional.of(itemStock));

        inventoryService.decreaseStock(1L, 999);

        assertThat(itemStock.getQuantity()).isEqualTo(0);
        verify(itemStockRepository).save(itemStock);
    }

    @Test
    @DisplayName("decreaseStock - quantity null no hace nada")
    void decreaseStock_nullQuantity_doesNothing() {
        inventoryService.decreaseStock(1L, null);
        verify(itemStockRepository, never()).save(any());
    }

    @Test
    @DisplayName("decreaseStock - quantity <= 0 no hace nada")
    void decreaseStock_zeroOrNegative_doesNothing() {
        inventoryService.decreaseStock(1L, 0);
        inventoryService.decreaseStock(1L, -1);
        verify(itemStockRepository, never()).save(any());
    }

    @Test
    @DisplayName("decreaseStock - itemStock no encontrado lanza NotFoundException")
    void decreaseStock_notFound_throws() {
        when(itemStockRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.decreaseStock(99L, 5))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("ItemStock not found with id: 99");
    }
}