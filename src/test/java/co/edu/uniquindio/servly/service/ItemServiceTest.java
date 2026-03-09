package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Inventory.ItemCreateRequest;
import co.edu.uniquindio.servly.DTO.Inventory.ItemDTO;
import co.edu.uniquindio.servly.DTO.Inventory.ItemUpdateRequest;
import co.edu.uniquindio.servly.DTO.Inventory.PaginatedItemResponse;
import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.model.entity.Inventory;
import co.edu.uniquindio.servly.model.entity.Item;
import co.edu.uniquindio.servly.model.entity.ItemCategory;
import co.edu.uniquindio.servly.model.entity.ItemStock;
import co.edu.uniquindio.servly.repository.*;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemService Tests")
class ItemServiceTest {

    @Mock private ItemRepository itemRepository;
    @Mock private ItemCategoryRepository itemCategoryRepository;
    @Mock private ItemStockRepository itemStockRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private InventoryRepository inventoryRepository;

    @InjectMocks
    private ItemService itemService;

    // ── fixtures ──────────────────────────────────────────────────────────────
    private ItemCategory category;
    private Item item;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        category = ItemCategory.builder()
                .id(1L)
                .name("Limpieza")
                .build();

        item = Item.builder()
                .id(1L)
                .name("Escoba")
                .description("Escoba de cerda")
                .unitOfMeasurement("unidad")
                .expirationDays(0)
                .itemCategory(category)
                .active(true)
                .idealStock(10)
                .build();

        inventory = Inventory.builder()
                .id(1L)
                .build();
    }

    // =========================================================================
    // getAllItems
    // =========================================================================
    @Nested
    @DisplayName("getAllItems()")
    class GetAllItems {

        @Test
        @DisplayName("Debe retornar lista de ItemDTO cuando existen items activos")
        void shouldReturnItemDTOList() {
            when(itemRepository.findAllActive()).thenReturn(List.of(item));

            List<ItemDTO> result = itemService.getAllItems();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            assertThat(result.get(0).getName()).isEqualTo("Escoba");
            verify(itemRepository).findAllActive();
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay items activos")
        void shouldReturnEmptyList() {
            when(itemRepository.findAllActive()).thenReturn(List.of());

            List<ItemDTO> result = itemService.getAllItems();

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // getAllItemsPaginated
    // =========================================================================
    @Nested
    @DisplayName("getAllItemsPaginated()")
    class GetAllItemsPaginated {

        @Test
        @DisplayName("Debe retornar respuesta paginada correctamente")
        void shouldReturnPaginatedResponse() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Item> page = new PageImpl<>(List.of(item), pageable, 1);
            when(itemRepository.findAllActivePaginated(pageable)).thenReturn(page);

            PaginatedItemResponse response = itemService.getAllItemsPaginated(pageable);

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getPageNumber()).isZero();
            assertThat(response.getPageSize()).isEqualTo(10);
            assertThat(response.getTotalElements()).isEqualTo(1L);
            assertThat(response.getTotalPages()).isEqualTo(1);
            assertThat(response.isLast()).isTrue();
        }

        @Test
        @DisplayName("Debe retornar página vacía cuando no hay resultados")
        void shouldReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Item> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(itemRepository.findAllActivePaginated(pageable)).thenReturn(emptyPage);

            PaginatedItemResponse response = itemService.getAllItemsPaginated(pageable);

            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isZero();
        }
    }

    // =========================================================================
    // getItemById
    // =========================================================================
    @Nested
    @DisplayName("getItemById()")
    class GetItemById {

        @Test
        @DisplayName("Debe retornar ItemDTO cuando el item existe")
        void shouldReturnItemDTO() {
            when(itemRepository.findByIdActive(1L)).thenReturn(Optional.of(item));

            ItemDTO result = itemService.getItemById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Escoba");
            assertThat(result.getCategory()).isEqualTo("Limpieza");
        }

        @Test
        @DisplayName("Debe lanzar AuthException cuando el item no existe")
        void shouldThrowWhenNotFound() {
            when(itemRepository.findByIdActive(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.getItemById(99L))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Item no encontrado");
        }
    }

    // =========================================================================
    // getItemsByCategory
    // =========================================================================
    @Nested
    @DisplayName("getItemsByCategory()")
    class GetItemsByCategory {

        @Test
        @DisplayName("Debe retornar items de la categoría solicitada")
        void shouldReturnItemsByCategory() {
            when(itemRepository.findByCategoryId(1L)).thenReturn(List.of(item));

            List<ItemDTO> result = itemService.getItemsByCategory(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isEqualTo("Limpieza");
        }

        @Test
        @DisplayName("Debe retornar lista vacía si la categoría no tiene items")
        void shouldReturnEmptyForUnknownCategory() {
            when(itemRepository.findByCategoryId(99L)).thenReturn(List.of());

            assertThat(itemService.getItemsByCategory(99L)).isEmpty();
        }
    }

    // =========================================================================
    // getItemsByCategoryPaginated
    // =========================================================================
    @Nested
    @DisplayName("getItemsByCategoryPaginated()")
    class GetItemsByCategoryPaginated {

        @Test
        @DisplayName("Debe retornar respuesta paginada por categoría")
        void shouldReturnPaginatedByCategory() {
            Pageable pageable = PageRequest.of(0, 5);
            Page<Item> page = new PageImpl<>(List.of(item), pageable, 1);
            when(itemRepository.findByCategoryIdPaginated(1L, pageable)).thenReturn(page);

            PaginatedItemResponse response = itemService.getItemsByCategoryPaginated(1L, pageable);

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1L);
        }
    }

    // =========================================================================
    // searchItems
    // =========================================================================
    @Nested
    @DisplayName("searchItems()")
    class SearchItems {

        @Test
        @DisplayName("Debe retornar items cuyo nombre coincide")
        void shouldReturnMatchingItems() {
            when(itemRepository.findByNameContaining("Esc")).thenReturn(List.of(item));

            List<ItemDTO> result = itemService.searchItems("Esc");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).contains("Escoba");
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay coincidencias")
        void shouldReturnEmptyWhenNoMatch() {
            when(itemRepository.findByNameContaining("XYZ")).thenReturn(List.of());

            assertThat(itemService.searchItems("XYZ")).isEmpty();
        }
    }

    // =========================================================================
    // searchItemsPaginated
    // =========================================================================
    @Nested
    @DisplayName("searchItemsPaginated()")
    class SearchItemsPaginated {

        @Test
        @DisplayName("Debe retornar página de resultados de búsqueda")
        void shouldReturnPaginatedSearchResults() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Item> page = new PageImpl<>(List.of(item), pageable, 1);
            when(itemRepository.findByNameContainingPaginated("Escoba", pageable)).thenReturn(page);

            PaginatedItemResponse response = itemService.searchItemsPaginated("Escoba", pageable);

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1L);
        }
    }

    // =========================================================================
    // createItem
    // =========================================================================
    @Nested
    @DisplayName("createItem()")
    class CreateItem {

        private ItemCreateRequest buildRequest() {
            ItemCreateRequest req = new ItemCreateRequest();
            req.setName("Trapeador");
            req.setDescription("Trapeador industrial");
            req.setUnitOfMeasurement("unidad");
            req.setExpirationDays(0);
            req.setCategory("1");
            req.setIdealStock(5);
            return req;
        }

        @Test
        @DisplayName("Debe crear item y retornar su DTO cuando la categoría existe")
        void shouldCreateItemSuccessfully() {
            ItemCreateRequest request = buildRequest();

            Item savedItem = Item.builder()
                    .id(2L)
                    .name("Trapeador")
                    .description("Trapeador industrial")
                    .unitOfMeasurement("unidad")
                    .expirationDays(0)
                    .itemCategory(category)
                    .active(true)
                    .idealStock(5)
                    .build();

            when(itemCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(itemRepository.saveAndFlush(any(Item.class))).thenReturn(savedItem);
            when(inventoryRepository.findAll()).thenReturn(List.of(inventory));
            when(itemStockRepository.save(any(ItemStock.class))).thenAnswer(inv -> inv.getArgument(0));

            ItemDTO result = itemService.createItem(request);

            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getName()).isEqualTo("Trapeador");
            assertThat(result.getIdealStock()).isEqualTo(5);
            verify(itemStockRepository).save(any(ItemStock.class));
        }


        @Test
        @DisplayName("Debe lanzar AuthException si la categoría no existe")
        void shouldThrowWhenCategoryNotFound() {
            ItemCreateRequest request = buildRequest();
            request.setCategory("99");

            when(itemCategoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.createItem(request))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Categoría no encontrada");

            verify(itemRepository, never()).saveAndFlush(any());
        }


        // =========================================================================
        // updateItem
        // =========================================================================
        @Nested
        @DisplayName("updateItem()")
        class UpdateItem {

            @Test
            @DisplayName("Debe actualizar todos los campos provistos")
            void shouldUpdateAllFields() {
                ItemUpdateRequest request = new ItemUpdateRequest();
                request.setName("Escoba nueva");
                request.setDescription("Descripción actualizada");
                request.setUnitOfMeasurement("pieza");
                request.setExpirationDays(30);
                request.setIdealStock(20);
                request.setCategory("1");

                when(itemRepository.findByIdActive(1L)).thenReturn(Optional.of(item));
                when(itemCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
                when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

                ItemDTO result = itemService.updateItem(1L, request);

                assertThat(result.getName()).isEqualTo("Escoba nueva");
                assertThat(result.getDescription()).isEqualTo("Descripción actualizada");
                assertThat(result.getUnitOfMeasurement()).isEqualTo("pieza");
                assertThat(result.getExpirationDays()).isEqualTo(30);
                assertThat(result.getIdealStock()).isEqualTo(20);
            }

            @Test
            @DisplayName("Debe ignorar campos nulos o vacíos")
            void shouldIgnoreNullAndBlankFields() {
                ItemUpdateRequest request = new ItemUpdateRequest();
                request.setName(null);
                request.setDescription(null);
                request.setUnitOfMeasurement("  ");
                request.setExpirationDays(null);
                request.setIdealStock(null);
                request.setCategory(null);

                when(itemRepository.findByIdActive(1L)).thenReturn(Optional.of(item));
                when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

                ItemDTO result = itemService.updateItem(1L, request);

                // Valores originales intactos
                assertThat(result.getName()).isEqualTo("Escoba");
                assertThat(result.getIdealStock()).isEqualTo(10);
                verify(itemCategoryRepository, never()).findById(anyLong());
            }

            @Test
            @DisplayName("Debe lanzar AuthException si el item no existe")
            void shouldThrowWhenItemNotFound() {
                when(itemRepository.findByIdActive(99L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> itemService.updateItem(99L, new ItemUpdateRequest()))
                        .isInstanceOf(AuthException.class)
                        .hasMessageContaining("Item no encontrado");
            }

            @Test
            @DisplayName("Debe lanzar AuthException si la nueva categoría no existe")
            void shouldThrowWhenNewCategoryNotFound() {
                ItemUpdateRequest request = new ItemUpdateRequest();
                request.setCategory("88");

                when(itemRepository.findByIdActive(1L)).thenReturn(Optional.of(item));
                when(itemCategoryRepository.findById(88L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> itemService.updateItem(1L, request))
                        .isInstanceOf(AuthException.class)
                        .hasMessageContaining("Categoría no encontrada");
            }
        }
    }


}