package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Inventory.CreateItemCategoryRequest;
import co.edu.uniquindio.servly.DTO.Inventory.ItemCategoryResponse;
import co.edu.uniquindio.servly.DTO.Inventory.PaginatedItemCategoryResponse;
import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.model.entity.ItemCategory;
import co.edu.uniquindio.servly.repository.ItemCategoryRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemCategoryService Tests")
class ItemCategoryServiceTest {

    @Mock
    private ItemCategoryRepository categoryRepository;

    @InjectMocks
    private ItemCategoryService itemCategoryService;

    // ── fixtures ──────────────────────────────────────────────────────────────
    private ItemCategory activeCategory;
    private CreateItemCategoryRequest validRequest;

    @BeforeEach
    void setUp() {
        activeCategory = ItemCategory.builder()
                .id(1L)
                .name("Limpieza")
                .description("Productos de limpieza")
                .active(true)
                .deleted(false)
                .build();

        validRequest = new CreateItemCategoryRequest();
        validRequest.setName("Limpieza");
        validRequest.setDescription("Productos de limpieza");
    }

    // =========================================================================
    // createCategory
    // =========================================================================
    @Nested
    @DisplayName("createCategory()")
    class CreateCategory {

        @Test
        @DisplayName("Debe crear categoría y retornar su respuesta cuando el nombre es único")
        void shouldCreateCategorySuccessfully() {
            when(categoryRepository.existsByName("Limpieza")).thenReturn(false);
            when(categoryRepository.save(any(ItemCategory.class))).thenReturn(activeCategory);

            ItemCategoryResponse result = itemCategoryService.createCategory(validRequest);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Limpieza");
            assertThat(result.getDescription()).isEqualTo("Productos de limpieza");
            verify(categoryRepository).save(any(ItemCategory.class));
        }

        @Test
        @DisplayName("La categoría creada debe estar activa por defecto")
        void shouldCreateCategoryAsActiveByDefault() {
            when(categoryRepository.existsByName("Limpieza")).thenReturn(false);
            when(categoryRepository.save(any(ItemCategory.class))).thenReturn(activeCategory);

            ItemCategoryResponse result = itemCategoryService.createCategory(validRequest);

            assertThat(result.getActive()).isTrue();
        }

        @Test
        @DisplayName("Debe lanzar AuthException cuando ya existe una categoría con ese nombre")
        void shouldThrowWhenNameAlreadyExists() {
            when(categoryRepository.existsByName("Limpieza")).thenReturn(true);

            assertThatThrownBy(() -> itemCategoryService.createCategory(validRequest))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Ya existe una categoría con el nombre: Limpieza");

            verify(categoryRepository, never()).save(any());
        }
    }

    // =========================================================================
    // getAllCategories
    // =========================================================================
    @Nested
    @DisplayName("getAllCategories()")
    class GetAllCategories {

        @Test
        @DisplayName("Debe retornar lista de todas las categorías")
        void shouldReturnAllCategories() {
            ItemCategory second = ItemCategory.builder()
                    .id(2L).name("Herramientas").description("Herramientas varias")
                    .active(true).deleted(false).build();

            when(categoryRepository.findAll()).thenReturn(List.of(activeCategory, second));

            List<ItemCategoryResponse> result = itemCategoryService.getAllCategories();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ItemCategoryResponse::getName)
                    .containsExactly("Limpieza", "Herramientas");
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay categorías")
        void shouldReturnEmptyListWhenNoCategories() {
            when(categoryRepository.findAll()).thenReturn(List.of());

            assertThat(itemCategoryService.getAllCategories()).isEmpty();
        }
    }

    // =========================================================================
    // getAllCategoriesPaginated
    // =========================================================================
    @Nested
    @DisplayName("getAllCategoriesPaginated()")
    class GetAllCategoriesPaginated {

        @Test
        @DisplayName("Debe retornar respuesta paginada correctamente")
        void shouldReturnPaginatedResponse() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<ItemCategory> page = new PageImpl<>(List.of(activeCategory), pageable, 1);
            when(categoryRepository.findAllPaginated(pageable)).thenReturn(page);

            PaginatedItemCategoryResponse response = itemCategoryService.getAllCategoriesPaginated(pageable);

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
            Page<ItemCategory> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(categoryRepository.findAllPaginated(pageable)).thenReturn(emptyPage);

            PaginatedItemCategoryResponse response = itemCategoryService.getAllCategoriesPaginated(pageable);

            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isZero();
        }
    }

    // =========================================================================
    // getCategoryById
    // =========================================================================
    @Nested
    @DisplayName("getCategoryById()")
    class GetCategoryById {

        @Test
        @DisplayName("Debe retornar la categoría cuando existe")
        void shouldReturnCategoryWhenFound() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory));

            ItemCategoryResponse result = itemCategoryService.getCategoryById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Limpieza");
        }

        @Test
        @DisplayName("Debe lanzar AuthException cuando la categoría no existe")
        void shouldThrowWhenCategoryNotFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemCategoryService.getCategoryById(99L))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("99");
        }
    }

    // =========================================================================
    // updateCategory
    // =========================================================================
    @Nested
    @DisplayName("updateCategory()")
    class UpdateCategory {

        @Test
        @DisplayName("Debe actualizar nombre y descripción cuando el nuevo nombre es único")
        void shouldUpdateSuccessfully() {
            CreateItemCategoryRequest request = new CreateItemCategoryRequest();
            request.setName("Herramientas");
            request.setDescription("Nueva descripción");

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory));
            when(categoryRepository.existsByName("Herramientas")).thenReturn(false);
            when(categoryRepository.save(any(ItemCategory.class))).thenAnswer(inv -> inv.getArgument(0));

            ItemCategoryResponse result = itemCategoryService.updateCategory(1L, request);

            assertThat(result.getName()).isEqualTo("Herramientas");
            assertThat(result.getDescription()).isEqualTo("Nueva descripción");
        }

        @Test
        @DisplayName("Debe actualizar sin verificar duplicado cuando el nombre no cambia")
        void shouldUpdateWithoutDuplicateCheckWhenNameUnchanged() {
            CreateItemCategoryRequest request = new CreateItemCategoryRequest();
            request.setName("Limpieza"); // mismo nombre
            request.setDescription("Descripción actualizada");

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory));
            when(categoryRepository.save(any(ItemCategory.class))).thenAnswer(inv -> inv.getArgument(0));

            itemCategoryService.updateCategory(1L, request);

            // existsByName NO debe ser llamado si el nombre es el mismo
            verify(categoryRepository, never()).existsByName(anyString());
        }

        @Test
        @DisplayName("Debe lanzar AuthException si la categoría no existe")
        void shouldThrowWhenCategoryNotFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemCategoryService.updateCategory(99L, validRequest))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("99");

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar AuthException si el nuevo nombre ya pertenece a otra categoría")
        void shouldThrowWhenNewNameIsDuplicate() {
            CreateItemCategoryRequest request = new CreateItemCategoryRequest();
            request.setName("Herramientas"); // nombre diferente pero ya existe
            request.setDescription("desc");

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory));
            when(categoryRepository.existsByName("Herramientas")).thenReturn(true);

            assertThatThrownBy(() -> itemCategoryService.updateCategory(1L, request))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Ya existe una categoría con el nombre: Herramientas");

            verify(categoryRepository, never()).save(any());
        }
    }

    // =========================================================================
    // toggleCategory
    // =========================================================================
    @Nested
    @DisplayName("toggleCategory()")
    class ToggleCategory {

        @Test
        @DisplayName("Debe desactivar una categoría activa")
        void shouldDeactivateActiveCategory() {
            activeCategory.setActive(true);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory));
            when(categoryRepository.save(any(ItemCategory.class))).thenAnswer(inv -> inv.getArgument(0));

            ItemCategoryResponse result = itemCategoryService.toggleCategory(1L);

            assertThat(result.getActive()).isFalse();
        }

        @Test
        @DisplayName("Debe activar una categoría inactiva")
        void shouldActivateInactiveCategory() {
            activeCategory.setActive(false);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory));
            when(categoryRepository.save(any(ItemCategory.class))).thenAnswer(inv -> inv.getArgument(0));

            ItemCategoryResponse result = itemCategoryService.toggleCategory(1L);

            assertThat(result.getActive()).isTrue();        }

        @Test
        @DisplayName("Debe lanzar AuthException si la categoría no existe")
        void shouldThrowWhenCategoryNotFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemCategoryService.toggleCategory(99L))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("99");

            verify(categoryRepository, never()).save(any());
        }
    }

    // =========================================================================
    // deleteCategory
    // =========================================================================
    @Nested
    @DisplayName("deleteCategory()")
    class DeleteCategory {

        @Test
        @DisplayName("Debe marcar la categoría como eliminada (soft delete)")
        void shouldSoftDeleteCategory() {
            when(categoryRepository.findByIdIncludingDeleted(1L)).thenReturn(Optional.of(activeCategory));
            when(categoryRepository.save(any(ItemCategory.class))).thenAnswer(inv -> inv.getArgument(0));

            itemCategoryService.deleteCategory(1L);

            assertThat(activeCategory.getDeleted()).isTrue();
            assertThat(activeCategory.getDeletedAt()).isNotNull();
            assertThat(activeCategory.getDeletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
            verify(categoryRepository).save(activeCategory);
        }

        @Test
        @DisplayName("Debe lanzar AuthException si la categoría no existe")
        void shouldThrowWhenCategoryNotFound() {
            when(categoryRepository.findByIdIncludingDeleted(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemCategoryService.deleteCategory(99L))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("99");

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe permitir soft delete de una categoría ya inactiva")
        void shouldAllowDeletingInactiveCategory() {
            activeCategory.setActive(false);
            when(categoryRepository.findByIdIncludingDeleted(1L)).thenReturn(Optional.of(activeCategory));
            when(categoryRepository.save(any(ItemCategory.class))).thenAnswer(inv -> inv.getArgument(0));

            itemCategoryService.deleteCategory(1L);

            assertThat(activeCategory.getDeleted()).isTrue();
            verify(categoryRepository).save(activeCategory);
        }
    }
}