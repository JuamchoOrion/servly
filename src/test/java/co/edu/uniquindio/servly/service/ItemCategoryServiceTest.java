package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Inventory.CreateItemCategoryRequest;
import co.edu.uniquindio.servly.DTO.Inventory.ItemCategoryResponse;
import co.edu.uniquindio.servly.DTO.Inventory.PaginatedItemCategoryResponse;
import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.model.entity.ItemCategory;
import co.edu.uniquindio.servly.repository.ItemCategoryRepository;
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
@DisplayName("ItemCategoryService Tests")
class ItemCategoryServiceTest {

    @Mock ItemCategoryRepository categoryRepository;

    @InjectMocks ItemCategoryService itemCategoryService;

    private ItemCategory category;
    private CreateItemCategoryRequest request;

    @BeforeEach
    void setUp() {
        category = ItemCategory.builder()
                .id(1L)
                .name("Granos")
                .description("Arroz, lentejas, etc.")
                .active(true)
                .build();

        request = new CreateItemCategoryRequest();
        request.setName("Granos");
        request.setDescription("Arroz, lentejas, etc.");
    }

    // ── createCategory ────────────────────────────────────────────────────

    @Test
    @DisplayName("createCategory - crea categoría correctamente")
    void createCategory_success() {
        when(categoryRepository.existsByName("Granos")).thenReturn(false);
        when(categoryRepository.save(any())).thenReturn(category);

        ItemCategoryResponse response = itemCategoryService.createCategory(request);

        assertThat(response.getName()).isEqualTo("Granos");
        assertThat(response.getDescription()).isEqualTo("Arroz, lentejas, etc.");
        verify(categoryRepository).save(any(ItemCategory.class));
    }

    @Test
    @DisplayName("createCategory - nombre duplicado lanza AuthException")
    void createCategory_duplicateName_throws() {
        when(categoryRepository.existsByName("Granos")).thenReturn(true);

        assertThatThrownBy(() -> itemCategoryService.createCategory(request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Ya existe una categoría con el nombre: Granos");

        verify(categoryRepository, never()).save(any());
    }

    // ── getAllCategories ───────────────────────────────────────────────────

    @Test
    @DisplayName("getAllCategories - retorna todas las categorías")
    void getAllCategories_success() {
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        List<ItemCategoryResponse> result = itemCategoryService.getAllCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Granos");
    }

    @Test
    @DisplayName("getAllCategories - lista vacía retorna lista vacía")
    void getAllCategories_empty() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        List<ItemCategoryResponse> result = itemCategoryService.getAllCategories();

        assertThat(result).isEmpty();
    }

    // ── getAllCategoriesPaginated ──────────────────────────────────────────

    @Test
    @DisplayName("getAllCategoriesPaginated - retorna respuesta paginada")
    void getAllCategoriesPaginated_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ItemCategory> page = new PageImpl<>(List.of(category), pageable, 1);

        when(categoryRepository.findAllPaginated(pageable)).thenReturn(page);

        PaginatedItemCategoryResponse response = itemCategoryService.getAllCategoriesPaginated(pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1L);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.getPageNumber()).isEqualTo(0);
        assertThat(response.isLast()).isTrue();
    }

    // ── getCategoryById ───────────────────────────────────────────────────

    @Test
    @DisplayName("getCategoryById - retorna categoría existente")
    void getCategoryById_success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        ItemCategoryResponse response = itemCategoryService.getCategoryById(1L);

        assertThat(response.getName()).isEqualTo("Granos");
    }

    @Test
    @DisplayName("getCategoryById - ID no encontrado lanza AuthException")
    void getCategoryById_notFound_throws() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemCategoryService.getCategoryById(99L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Categoría de items no encontrada con ID: 99");
    }

    // ── updateCategory ────────────────────────────────────────────────────

    @Test
    @DisplayName("updateCategory - actualiza nombre y descripción")
    void updateCategory_success() {
        CreateItemCategoryRequest updateReq = new CreateItemCategoryRequest();
        updateReq.setName("Lácteos");
        updateReq.setDescription("Leche, queso, etc.");

        ItemCategory updated = ItemCategory.builder()
                .id(1L).name("Lácteos").description("Leche, queso, etc.").active(true).build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByName("Lácteos")).thenReturn(false);
        when(categoryRepository.save(any())).thenReturn(updated);

        ItemCategoryResponse response = itemCategoryService.updateCategory(1L, updateReq);

        assertThat(response.getName()).isEqualTo("Lácteos");
    }

    @Test
    @DisplayName("updateCategory - mismo nombre no valida duplicado")
    void updateCategory_sameName_noValidation() {
        // Si el nombre no cambió no debe verificar existsByName
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any())).thenReturn(category);

        ItemCategoryResponse response = itemCategoryService.updateCategory(1L, request);

        assertThat(response.getName()).isEqualTo("Granos");
        verify(categoryRepository, never()).existsByName(any());
    }

    @Test
    @DisplayName("updateCategory - nuevo nombre duplicado lanza AuthException")
    void updateCategory_duplicateNewName_throws() {
        CreateItemCategoryRequest updateReq = new CreateItemCategoryRequest();
        updateReq.setName("Lácteos");
        updateReq.setDescription("desc");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByName("Lácteos")).thenReturn(true);

        assertThatThrownBy(() -> itemCategoryService.updateCategory(1L, updateReq))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Ya existe una categoría con el nombre: Lácteos");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateCategory - ID no encontrado lanza AuthException")
    void updateCategory_notFound_throws() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemCategoryService.updateCategory(99L, request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Categoría de items no encontrada con ID: 99");
    }

    // ── toggleCategory ────────────────────────────────────────────────────

    @Test
    @DisplayName("toggleCategory - activa a desactiva")
    void toggleCategory_activeToInactive() {
        category.setActive(true);
        ItemCategory toggled = ItemCategory.builder()
                .id(1L).name("Granos").description("desc").active(false).build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any())).thenReturn(toggled);

        ItemCategoryResponse response = itemCategoryService.toggleCategory(1L);

        assertThat(response.getActive()).isFalse();
    }

    @Test
    @DisplayName("toggleCategory - inactiva a activa")
    void toggleCategory_inactiveToActive() {
        category.setActive(false);
        ItemCategory toggled = ItemCategory.builder()
                .id(1L).name("Granos").description("desc").active(true).build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any())).thenReturn(toggled);

        ItemCategoryResponse response = itemCategoryService.toggleCategory(1L);

        assertThat(response.getActive()).isTrue();
    }

    @Test
    @DisplayName("toggleCategory - ID no encontrado lanza AuthException")
    void toggleCategory_notFound_throws() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemCategoryService.toggleCategory(99L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Categoría de items no encontrada con ID: 99");
    }

    // ── deleteCategory ────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteCategory - soft delete marca deletedAt y save")
    void deleteCategory_success() {
        category.setDeletedAt(null);
        when(categoryRepository.findByIdIncludingDeleted(1L)).thenReturn(Optional.of(category));

        itemCategoryService.deleteCategory(1L);

        assertThat(category.getDeleted()).isTrue();
        assertThat(category.getDeletedAt()).isNotNull();
        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("deleteCategory - ID no encontrado lanza AuthException")
    void deleteCategory_notFound_throws() {
        when(categoryRepository.findByIdIncludingDeleted(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemCategoryService.deleteCategory(99L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Categoría de items no encontrada con ID: 99");
    }
}