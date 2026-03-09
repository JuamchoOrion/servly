package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Inventory.ItemStockDTO;
import co.edu.uniquindio.servly.DTO.Inventory.PaginatedInventoryResponse;
import co.edu.uniquindio.servly.exception.NotFoundException;
import co.edu.uniquindio.servly.model.entity.*;
import co.edu.uniquindio.servly.repository.InventoryRepository;
import co.edu.uniquindio.servly.repository.ItemStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryServiceImpl Tests")
class InventoryServiceImplTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private ItemStockRepository itemStockRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    // ── fixtures ──────────────────────────────────────────────────────────────
    private Inventory inventory;
    private ItemCategory category;
    private Item item;
    private Supplier supplier;
    private ItemStock itemStock;

    @BeforeEach
    void setUp() {
        inventory = Inventory.builder()
                .id(1L)
                .build();

        category = ItemCategory.builder()
                .id(1L)
                .name("Limpieza")
                .build();

        item = Item.builder()
                .id(1L)
                .name("Escoba")
                .description("Escoba industrial")
                .unitOfMeasurement("unidad")
                .expirationDays(0)
                .idealStock(10)
                .itemCategory(category)
                .build();

        supplier = Supplier.builder()
                .id(1L)
                .name("Proveedor ABC")
                .build();

        itemStock = ItemStock.builder()
                .id(1L)
                .inventory(inventory)
                .item(item)
                .supplier(supplier)
                .quantity(5)
                .build();
    }

    // =========================================================================
    // getInventory
    // =========================================================================
    @Nested
    @DisplayName("getInventory()")
    class GetInventory {

        @Test
        @DisplayName("Debe retornar lista de ItemStockDTO cuando el inventario existe")
        void shouldReturnItemStockDTOList() {
            when(inventoryRepository.findAll()).thenReturn(List.of(inventory));
            when(itemStockRepository.findByInventoryId(1L)).thenReturn(List.of(itemStock));

            List<ItemStockDTO> result = inventoryService.getInventory();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getItemStockId()).isEqualTo(1L);
            assertThat(result.get(0).getName()).isEqualTo("Escoba");
            assertThat(result.get(0).getCategory()).isEqualTo("Limpieza");
            assertThat(result.get(0).getQuantity()).isEqualTo(5);
            assertThat(result.get(0).getSupplierName()).isEqualTo("Proveedor ABC");
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando el inventario no tiene stocks")
        void shouldReturnEmptyListWhenNoStocks() {
            when(inventoryRepository.findAll()).thenReturn(List.of(inventory));
            when(itemStockRepository.findByInventoryId(1L)).thenReturn(List.of());

            assertThat(inventoryService.getInventory()).isEmpty();
        }

        @Test
        @DisplayName("Debe lanzar NotFoundException cuando no existe inventario")
        void shouldThrowWhenNoInventoryFound() {
            when(inventoryRepository.findAll()).thenReturn(List.of());

            assertThatThrownBy(() -> inventoryService.getInventory())
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("No inventory found");

            verify(itemStockRepository, never()).findByInventoryId(anyLong());
        }

        @Test
        @DisplayName("Debe mapear correctamente un ItemStock con item null")
        void shouldMapItemStockWithNullItem() {
            ItemStock stockWithoutItem = ItemStock.builder()
                    .id(2L)
                    .inventory(inventory)
                    .item(null)
                    .supplier(null)
                    .quantity(0)
                    .build();

            when(inventoryRepository.findAll()).thenReturn(List.of(inventory));
            when(itemStockRepository.findByInventoryId(1L)).thenReturn(List.of(stockWithoutItem));

            List<ItemStockDTO> result = inventoryService.getInventory();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isNull();
            assertThat(result.get(0).getCategory()).isNull();
            assertThat(result.get(0).getSupplierName()).isNull();
        }
    }

    // =========================================================================
    // getInventoryPaginated
    // =========================================================================
    @Nested
    @DisplayName("getInventoryPaginated()")
    class GetInventoryPaginated {

        @Test
        @DisplayName("Debe retornar respuesta paginada correctamente")
        void shouldReturnPaginatedResponse() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<ItemStock> page = new PageImpl<>(List.of(itemStock), pageable, 1);

            when(inventoryRepository.findAll()).thenReturn(List.of(inventory));
            when(itemStockRepository.findByInventoryId(1L, pageable)).thenReturn(page);

            PaginatedInventoryResponse response = inventoryService.getInventoryPaginated(pageable);

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getPageNumber()).isZero();
            assertThat(response.getPageSize()).isEqualTo(10);
            assertThat(response.getTotalElements()).isEqualTo(1L);
            assertThat(response.getTotalPages()).isEqualTo(1);
            assertThat(response.isLast()).isTrue();
        }

        @Test
        @DisplayName("Debe retornar página vacía cuando no hay stocks")
        void shouldReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<ItemStock> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(inventoryRepository.findAll()).thenReturn(List.of(inventory));
            when(itemStockRepository.findByInventoryId(1L, pageable)).thenReturn(emptyPage);

            PaginatedInventoryResponse response = inventoryService.getInventoryPaginated(pageable);

            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Debe lanzar NotFoundException cuando no existe inventario")
        void shouldThrowWhenNoInventoryFound() {
            when(inventoryRepository.findAll()).thenReturn(List.of());

            assertThatThrownBy(() -> inventoryService.getInventoryPaginated(PageRequest.of(0, 10)))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("No inventory found");
        }
    }

    // =========================================================================
    // increaseStock
    // =========================================================================
    @Nested
    @DisplayName("increaseStock()")
    class IncreaseStock {

        @Test
        @DisplayName("Debe incrementar la cantidad correctamente")
        void shouldIncreaseStockQuantity() {
            when(itemStockRepository.findById(1L)).thenReturn(Optional.of(itemStock));
            when(itemStockRepository.save(any(ItemStock.class))).thenAnswer(inv -> inv.getArgument(0));

            inventoryService.increaseStock(1L, 3);

            assertThat(itemStock.getQuantity()).isEqualTo(8); // 5 + 3
            verify(itemStockRepository).save(itemStock);
        }

        @Test
        @DisplayName("Debe ignorar la operación cuando quantity es null")
        void shouldDoNothingWhenQuantityIsNull() {
            inventoryService.increaseStock(1L, null);

            verify(itemStockRepository, never()).findById(anyLong());
            verify(itemStockRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe ignorar la operación cuando quantity es 0")
        void shouldDoNothingWhenQuantityIsZero() {
            inventoryService.increaseStock(1L, 0);

            verify(itemStockRepository, never()).findById(anyLong());
            verify(itemStockRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe ignorar la operación cuando quantity es negativo")
        void shouldDoNothingWhenQuantityIsNegative() {
            inventoryService.increaseStock(1L, -5);

            verify(itemStockRepository, never()).findById(anyLong());
            verify(itemStockRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar NotFoundException cuando el ItemStock no existe")
        void shouldThrowWhenItemStockNotFound() {
            when(itemStockRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inventoryService.increaseStock(99L, 5))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("99");

            verify(itemStockRepository, never()).save(any());
        }
    }

    // =========================================================================
    // decreaseStock
    // =========================================================================
    @Nested
    @DisplayName("decreaseStock()")
    class DecreaseStock {

        @Test
        @DisplayName("Debe decrementar la cantidad correctamente")
        void shouldDecreaseStockQuantity() {
            when(itemStockRepository.findById(1L)).thenReturn(Optional.of(itemStock));
            when(itemStockRepository.save(any(ItemStock.class))).thenAnswer(inv -> inv.getArgument(0));

            inventoryService.decreaseStock(1L, 3);

            assertThat(itemStock.getQuantity()).isEqualTo(2); // 5 - 3
            verify(itemStockRepository).save(itemStock);
        }

        @Test
        @DisplayName("Debe dejar la cantidad en 0 cuando se decrementa más de lo disponible")
        void shouldSetQuantityToZeroWhenDecreaseExceedsStock() {
            when(itemStockRepository.findById(1L)).thenReturn(Optional.of(itemStock));
            when(itemStockRepository.save(any(ItemStock.class))).thenAnswer(inv -> inv.getArgument(0));

            inventoryService.decreaseStock(1L, 100);

            assertThat(itemStock.getQuantity()).isZero();
            verify(itemStockRepository).save(itemStock);
        }

        @Test
        @DisplayName("Debe dejar la cantidad en 0 cuando se decrementa exactamente el stock disponible")
        void shouldSetQuantityToZeroWhenDecreaseEqualsStock() {
            when(itemStockRepository.findById(1L)).thenReturn(Optional.of(itemStock));
            when(itemStockRepository.save(any(ItemStock.class))).thenAnswer(inv -> inv.getArgument(0));

            inventoryService.decreaseStock(1L, 5); // exactamente el stock

            assertThat(itemStock.getQuantity()).isZero();
            verify(itemStockRepository).save(itemStock);
        }

        @Test
        @DisplayName("Debe ignorar la operación cuando quantity es null")
        void shouldDoNothingWhenQuantityIsNull() {
            inventoryService.decreaseStock(1L, null);

            verify(itemStockRepository, never()).findById(anyLong());
            verify(itemStockRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe ignorar la operación cuando quantity es 0")
        void shouldDoNothingWhenQuantityIsZero() {
            inventoryService.decreaseStock(1L, 0);

            verify(itemStockRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Debe ignorar la operación cuando quantity es negativo")
        void shouldDoNothingWhenQuantityIsNegative() {
            inventoryService.decreaseStock(1L, -10);

            verify(itemStockRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Debe lanzar NotFoundException cuando el ItemStock no existe")
        void shouldThrowWhenItemStockNotFound() {
            when(itemStockRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inventoryService.decreaseStock(99L, 5))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("99");

            verify(itemStockRepository, never()).save(any());
        }
    }
}