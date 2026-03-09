package co.edu.uniquindio.servly.controller;

import co.edu.uniquindio.servly.DTO.Inventory.ItemCreateRequest;
import co.edu.uniquindio.servly.DTO.Inventory.ItemDTO;
import co.edu.uniquindio.servly.DTO.Inventory.ItemUpdateRequest;
import co.edu.uniquindio.servly.DTO.Inventory.PaginatedItemResponse;
import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@Slf4j
public class ItemController {

    private final ItemService itemService;

    /**
     * GET /api/items
     * Obtiene todos los items activos
     * Acceso: ADMIN, STOREKEEPER
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<List<ItemDTO>> getAllItems() {
        log.info("GET /api/items - Obteniendo todos los items");
        List<ItemDTO> items = itemService.getAllItems();
        return ResponseEntity.ok(items);
    }

    /**
     * GET /api/items/paginated
     * Obtiene todos los items activos con paginación
     * Parámetros:
     * - page: número de página (0-indexed, default: 0)
     * - size: cantidad de items por página (default: 10)
     * - sort: ordenamiento (ej: "id,desc" o "name,asc")
     * Acceso: ADMIN, STOREKEEPER
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<PaginatedItemResponse> getAllItemsPaginated(
            @PageableDefault(size = 10, page = 0, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        log.info("GET /api/items/paginated - Obteniendo items paginados");
        PaginatedItemResponse response = itemService.getAllItemsPaginated(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/items/{id}
     * Obtiene un item específico por ID
     * Acceso: ADMIN, STOREKEEPER
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<ItemDTO> getItemById(@PathVariable Long id) {
        log.info("GET /api/items/{} - Obteniendo item por ID", id);
        ItemDTO item = itemService.getItemById(id);
        return ResponseEntity.ok(item);
    }

    /**
     * GET /api/items/category/{categoryId}
     * Obtiene items por categoría
     * Acceso: ADMIN, STOREKEEPER
     */
    @GetMapping("/category/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<List<ItemDTO>> getItemsByCategory(@PathVariable Long categoryId) {
        log.info("GET /api/items/category/{} - Obteniendo items por categoría", categoryId);
        List<ItemDTO> items = itemService.getItemsByCategory(categoryId);
        return ResponseEntity.ok(items);
    }

    /**
     * GET /api/items/category-paginated/{categoryId}
     * Obtiene items por categoría con paginación
     * Parámetros:
     * - page: número de página (0-indexed, default: 0)
     * - size: cantidad de items por página (default: 10)
     * - sort: ordenamiento (ej: "id,desc" o "name,asc")
     * Acceso: ADMIN, STOREKEEPER
     */
    @GetMapping("/category-paginated/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<PaginatedItemResponse> getItemsByCategoryPaginated(
            @PathVariable Long categoryId,
            @PageableDefault(size = 10, page = 0, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        log.info("GET /api/items/category-paginated/{} - Obteniendo items por categoría paginados", categoryId);
        PaginatedItemResponse response = itemService.getItemsByCategoryPaginated(categoryId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/items/search?name=...
     * Busca items por nombre
     * Acceso: ADMIN, STOREKEEPER
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<List<ItemDTO>> searchItems(@RequestParam String name) {
        log.info("GET /api/items/search?name={} - Buscando items", name);
        List<ItemDTO> items = itemService.searchItems(name);
        return ResponseEntity.ok(items);
    }

    /**
     * GET /api/items/search-paginated
     * Busca items por nombre con paginación
     * Parámetros:
     * - name: término de búsqueda
     * - page: número de página (0-indexed, default: 0)
     * - size: cantidad de items por página (default: 10)
     * - sort: ordenamiento (ej: "id,desc" o "name,asc")
     * Acceso: ADMIN, STOREKEEPER
     */
    @GetMapping("/search-paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<PaginatedItemResponse> searchItemsPaginated(
            @RequestParam String name,
            @PageableDefault(size = 10, page = 0, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        log.info("GET /api/items/search-paginated?name={} - Buscando items paginados", name);
        PaginatedItemResponse response = itemService.searchItemsPaginated(name, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/items
     * Crea un nuevo item
     * Acceso: ADMIN, STOREKEEPER
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<ItemDTO> createItem(@RequestBody ItemCreateRequest request) {
        log.info("POST /api/items - Creando nuevo item: {}", request.getName());
        ItemDTO createdItem = itemService.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
    }

    /**
     * PUT /api/items/{id}
     * Actualiza un item existente
     * Acceso: ADMIN, STOREKEEPER
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<ItemDTO> updateItem(@PathVariable Long id, @RequestBody ItemUpdateRequest request) {
        log.info("PUT /api/items/{} - Actualizando item", id);
        ItemDTO updatedItem = itemService.updateItem(id, request);
        return ResponseEntity.ok(updatedItem);
    }

    /**
     * DELETE /api/items/{id}
     * Desactiva (soft delete) un item
     * Acceso: ADMIN, STOREKEEPER
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER')")
    public ResponseEntity<MessageResponse> deleteItem(@PathVariable Long id) {
        log.info("DELETE /api/items/{} - Desactivando item", id);
        itemService.deleteItem(id);
        return ResponseEntity.ok(new MessageResponse("Item desactivado correctamente"));
    }
}

