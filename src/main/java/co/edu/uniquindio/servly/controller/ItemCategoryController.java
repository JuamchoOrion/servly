package co.edu.uniquindio.servly.controller;

import co.edu.uniquindio.servly.DTO.Inventory.CreateItemCategoryRequest;
import co.edu.uniquindio.servly.DTO.Inventory.ItemCategoryResponse;
import co.edu.uniquindio.servly.service.ItemCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints para gestionar categorías de items (inventario).
 * Disponibles para ADMIN y STOREKEEPER.
 */
@RestController
@RequestMapping("/api/item-categories")
@RequiredArgsConstructor
public class ItemCategoryController {

    private final ItemCategoryService categoryService;

    /**
     * Crea una nueva categoría de items.
     *
     * POST /api/item-categories
     * Authorization: Bearer {token}
     *
     * Body:
     * {
     *   "name": "Alimentos",
     *   "description": "Categoría para alimentos perecederos"
     * }
     *
     * Response: 200 OK
     * {
     *   "id": 1,
     *   "name": "Alimentos",
     *   "description": "Categoría para alimentos perecederos",
     *   "active": true
     * }
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STOREKEEPER')")
    public ResponseEntity<ItemCategoryResponse> createCategory(
            @Valid @RequestBody CreateItemCategoryRequest request) {
        return ResponseEntity.ok(categoryService.createCategory(request));
    }

    /**
     * Obtiene todas las categorías de items.
     *
     * GET /api/item-categories
     *
     * Response: 200 OK
     * [
     *   {
     *     "id": 1,
     *     "name": "Alimentos",
     *     "description": "Categoría para alimentos perecederos",
     *     "active": true
     *   },
     *   {
     *     "id": 2,
     *     "name": "Limpieza",
     *     "description": "Productos de limpieza",
     *     "active": true
     *   }
     * ]
     */
    @GetMapping
    public ResponseEntity<List<ItemCategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    /**
     * Obtiene una categoría por ID.
     *
     * GET /api/item-categories/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ItemCategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    /**
     * Actualiza una categoría existente.
     *
     * PUT /api/item-categories/{id}
     * Authorization: Bearer {token}
     *
     * Body:
     * {
     *   "name": "Alimentos Premium",
     *   "description": "Categoría para alimentos perecederos de calidad premium"
     * }
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STOREKEEPER')")
    public ResponseEntity<ItemCategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CreateItemCategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    /**
     * Activa o desactiva una categoría.
     *
     * PATCH /api/item-categories/{id}/toggle
     * Authorization: Bearer {token}
     */
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STOREKEEPER')")
    public ResponseEntity<ItemCategoryResponse> toggleCategory(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.toggleCategory(id));
    }

    /**
     * Elimina una categoría.
     * Solo si no tiene items asociados.
     *
     * DELETE /api/item-categories/{id}
     * Authorization: Bearer {token}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STOREKEEPER')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}

