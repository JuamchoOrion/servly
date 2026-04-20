package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Inventory.ItemDetailCreateRequest;
import co.edu.uniquindio.servly.DTO.Inventory.RecipeCreateRequest;
import co.edu.uniquindio.servly.DTO.Inventory.RecipeDTO;
import co.edu.uniquindio.servly.DTO.RecipeDetailDTO;
import co.edu.uniquindio.servly.DTO.Inventory.ItemDetailDTO;
import co.edu.uniquindio.servly.exception.NotFoundException;
import co.edu.uniquindio.servly.model.entity.Item;
import co.edu.uniquindio.servly.model.entity.ItemDetail;
import co.edu.uniquindio.servly.model.entity.Recipe;
import co.edu.uniquindio.servly.repository.ItemRepository;
import co.edu.uniquindio.servly.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar recetas y sus detalles (ItemDetails).
 *
 * Una receta contiene una lista de ItemDetails que especifican:
 * - Qué items se necesitan
 * - Cuánta cantidad de cada item
 * - Si el item es opcional
 * - Anotaciones y variaciones de cantidad
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final ItemRepository itemRepository;

    /**
     * Busca recetas por nombre si la query tiene más de 2 caracteres.
     * Si la query es nula o corta, devuelve todas las recetas.
     *
     * Usa eager loading FETCH para evitar LazyInitializationException
     * cuando se accede a itemDetailList en RecipeChatResponse
     */
    @Transactional(readOnly = true)
    public List<Recipe> searchRecipes(String query) {
        if (query != null && query.trim().length() > 2) {
            List<Recipe> found = recipeRepository.findByNameContainingIgnoreCaseEager(query.trim());
            if (!found.isEmpty()) {
                return found;
            }
        }
        return recipeRepository.findAllEager();
    }

    /**
     * Crea una nueva receta con sus ItemDetails.
     *
     * @param request Contiene nombre, descripción y lista de itemDetails
     * @return RecipeDTO con la receta creada
     */
    public RecipeDTO createRecipe(RecipeCreateRequest request) {
        if (request.getItemDetails() == null || request.getItemDetails().isEmpty()) {
            throw new IllegalArgumentException("Una receta debe tener al menos un item detail");
        }

        Recipe recipe = Recipe.builder()
                .name(request.getName())
                .quantity(request.getQuantity())
                .description(request.getDescription())
                .itemDetailList(new ArrayList<>())
                .build();

        // Crear los ItemDetails asociados
        for (ItemDetailCreateRequest itemDetailReq : request.getItemDetails()) {
            ItemDetail itemDetail = createItemDetail(recipe, itemDetailReq);
            recipe.getItemDetailList().add(itemDetail);
        }

        Recipe saved = recipeRepository.save(recipe);
        return RecipeDTO.fromEntity(saved);
    }

    /**
     * Obtiene todas las recetas.
     */
    @Transactional(readOnly = true)
    public List<RecipeDTO> getAllRecipes() {
        return recipeRepository.findAll().stream()
                .map(RecipeDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una receta por ID con detalles completos de items.
     */
    @Transactional(readOnly = true)
    public RecipeDetailDTO getRecipeById(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Receta no encontrada: " + id));

        return RecipeDetailDTO.builder()
                .id(recipe.getId())
                .name(recipe.getName())
                .quantity(recipe.getQuantity())
                .description(recipe.getDescription())
                .itemDetailList(recipe.getItemDetailList().stream()
                        .map(itemDetail -> ItemDetailDTO.builder()
                                .id(itemDetail.getId())
                                .quantity(itemDetail.getQuantity())
                                .annotation(itemDetail.getAnnotation())
                                .isOptional(itemDetail.getIsOptional())
                                .itemId(itemDetail.getItem() != null ? itemDetail.getItem().getId() : null)
                                .recipeId(itemDetail.getRecipe() != null ? itemDetail.getRecipe().getId() : null)
                                .build())
                        .toList())
                .build();
    }

    /**
     * Actualiza una receta y sus ItemDetails.
     *
     * @param id ID de la receta
     * @param request Nuevos datos de la receta
     * @return RecipeDTO actualizado
     */
    public RecipeDTO updateRecipe(Long id, RecipeCreateRequest request) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Receta no encontrada: " + id));

        recipe.setName(request.getName());
        recipe.setQuantity(request.getQuantity());
        recipe.setDescription(request.getDescription());

        // Limpiar y recrear ItemDetails
        recipe.getItemDetailList().clear();

        if (request.getItemDetails() != null && !request.getItemDetails().isEmpty()) {
            for (ItemDetailCreateRequest itemDetailReq : request.getItemDetails()) {
                ItemDetail itemDetail = createItemDetail(recipe, itemDetailReq);
                recipe.getItemDetailList().add(itemDetail);
            }
        }

        Recipe updated = recipeRepository.save(recipe);
        return RecipeDTO.fromEntity(updated);
    }

    /**
     * Soft delete de una receta.
     * En lugar de eliminar físicamente, marca la receta como DELETED.
     * Las recetas eliminadas no aparecerán en las consultas normales.
     */
    public void deleteRecipe(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Receta no encontrada: " + id));
        recipe.setDeleted(true);
        recipe.setDeletedAt(LocalDateTime.now());
        recipeRepository.save(recipe);
    }

    /**
     * Crea un ItemDetail a partir del request.
     * Valida que el Item exista y sea activo.
     */
    private ItemDetail createItemDetail(Recipe recipe, ItemDetailCreateRequest request) {
        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new NotFoundException("Item no encontrado: " + request.getItemId()));

        if (!item.getActive()) {
            throw new IllegalArgumentException("No puedes usar un item inactivo en una receta");
        }

        ItemDetail itemDetail = ItemDetail.builder()
                .recipe(recipe)
                .item(item)
                .quantity(request.getQuantity())
                .annotation(request.getAnnotation())
                .isOptional(request.getIsOptional() != null ? request.getIsOptional() : false)
                .minQuantity(request.getIsOptional() != null && request.getIsOptional() ? 0 : request.getQuantity())
                .maxQuantity(request.getQuantity())
                .build();

        return itemDetail;
    }

    /**
     * Agrega un ItemDetail a una receta existente.
     */
    public RecipeDTO addItemDetailToRecipe(Long recipeId, ItemDetailCreateRequest request) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new NotFoundException("Receta no encontrada: " + recipeId));

        ItemDetail itemDetail = createItemDetail(recipe, request);
        recipe.getItemDetailList().add(itemDetail);

        Recipe updated = recipeRepository.save(recipe);
        return RecipeDTO.fromEntity(updated);
    }

    /**
     * Elimina un ItemDetail de una receta.
     */
    public void removeItemDetailFromRecipe(Long recipeId, Long itemDetailId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new NotFoundException("Receta no encontrada: " + recipeId));

        recipe.getItemDetailList().removeIf(detail -> detail.getId().equals(itemDetailId));
        recipeRepository.save(recipe);
    }
}

