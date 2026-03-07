package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Inventory.CreateItemCategoryRequest;
import co.edu.uniquindio.servly.DTO.Inventory.ItemCategoryResponse;
import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.model.entity.ItemCategory;
import co.edu.uniquindio.servly.repository.ItemCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar categorías de items (inventario).
 * Disponible solo para ADMIN y STOREKEEPER.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ItemCategoryService {

    private final ItemCategoryRepository categoryRepository;

    /**
     * Crea una nueva categoría de items.
     * El nombre debe ser único.
     */
    public ItemCategoryResponse createCategory(CreateItemCategoryRequest request) {
        // Validar que no exista una categoría con ese nombre
        if (categoryRepository.existsByName(request.getName())) {
            throw new AuthException("Ya existe una categoría con el nombre: " + request.getName());
        }

        ItemCategory category = ItemCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(true)  // Se crea activa por defecto
                .build();

        ItemCategory saved = categoryRepository.save(category);
        log.info("Categoría de items creada: {}", saved.getName());
        return ItemCategoryResponse.from(saved);
    }

    /**
     * Obtiene todas las categorías de items.
     */
    @Transactional(readOnly = true)
    public List<ItemCategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(ItemCategoryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una categoría por ID.
     */
    @Transactional(readOnly = true)
    public ItemCategoryResponse getCategoryById(Long id) {
        ItemCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new AuthException("Categoría de items no encontrada con ID: " + id));
        return ItemCategoryResponse.from(category);
    }

    /**
     * Actualiza una categoría existente.
     */
    public ItemCategoryResponse updateCategory(Long id, CreateItemCategoryRequest request) {
        ItemCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new AuthException("Categoría de items no encontrada con ID: " + id));

        // Si el nombre cambió, validar que no exista otro con ese nombre
        if (!category.getName().equals(request.getName()) &&
            categoryRepository.existsByName(request.getName())) {
            throw new AuthException("Ya existe una categoría con el nombre: " + request.getName());
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        ItemCategory updated = categoryRepository.save(category);
        log.info("Categoría de items actualizada: {}", updated.getName());
        return ItemCategoryResponse.from(updated);
    }

    /**
     * Activa o desactiva una categoría.
     */
    public ItemCategoryResponse toggleCategory(Long id) {
        ItemCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new AuthException("Categoría de items no encontrada con ID: " + id));

        category.setActive(!category.getActive());
        ItemCategory updated = categoryRepository.save(category);
        log.info("Categoría de items {} : {}", updated.getName(), updated.getActive() ? "activada" : "desactivada");
        return ItemCategoryResponse.from(updated);
    }

    /**
     * Realiza soft delete de una categoría.
     * La categoría se marca como eliminada pero se conserva en la BD.
     */
    public void deleteCategory(Long id) {
        ItemCategory category = categoryRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new AuthException("Categoría de items no encontrada con ID: " + id));

        category.setDeleted(true);
        category.setDeletedAt(LocalDateTime.now());
        categoryRepository.save(category);
        log.info("Categoría de items eliminada (soft delete): {}", category.getName());
    }
}

