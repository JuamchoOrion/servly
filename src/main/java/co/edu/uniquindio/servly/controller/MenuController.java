package co.edu.uniquindio.servly.controller;

import co.edu.uniquindio.servly.DTO.Product.ProductWithRecipeDTO;
import co.edu.uniquindio.servly.model.entity.ProductCategory;
import co.edu.uniquindio.servly.service.ProductMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Page;

/**
 * Controlador público para que CLIENTES obtengan productos con opciones de variación
     * Mejor para: Menús pequeños que caben en una sola vista
     * GET /api/menu/products - Todos los productos en una lista
 *   → Respuesta: List<ProductDTO>
 *   → Sin parámetros de paginación
 *   → Para UI de menú estático o en una sola vista
 * GET /api/menu/products - Devuelve LISTA completa (sin paginar)
 *
 *   → Uso recomendado: Menú interactivo con muchos productos
 *   → Respuesta: Page<ProductDTO> con metadata (totalElements, totalPages, etc)
 *   → Parámetros: ?page=0&size=10
 *   → Para UI de scroll infinito o lazy loading
 * GET /api/products/active - Devuelve PÁGINA (paginado)
 *
 * DIFERENCIA ENTRE ENDPOINTS:
 *
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class MenuController {

    private final ProductMenuService productMenuService;

    /**
     * Obtener todos los productos disponibles con sus variaciones
     * GET /api/products/active
     * Acceso: Público (sin autenticación)
     */
    @GetMapping("/products/active")
    public ResponseEntity<Page<ProductWithRecipeDTO>> getActiveProducts(
            @PageableDefault(size = 10, page = 0, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        log.info("Cliente consultando productos activos con paginación");
        Page<ProductWithRecipeDTO> page = productMenuService.getActiveProductsPaginated(pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Obtener todos los productos disponibles con sus variaciones
     * GET /api/menu/products
     * Acceso: Público (sin autenticación)
     */
    @GetMapping("/menu/products")
    public ResponseEntity<List<ProductWithRecipeDTO>> getAvailableProducts() {
        log.info("Cliente consultando menú de productos");
        List<ProductWithRecipeDTO> dtos = productMenuService.getActiveProducts();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Obtener un producto específico con sus variaciones de items
     * GET /api/products/{id}
     * GET /api/menu/products/{id}
     * Acceso: Público
     */
    @GetMapping({"/products/{id}", "/menu/products/{id}"})
    public ResponseEntity<ProductWithRecipeDTO> getProduct(@PathVariable Long id) {
        log.info("Cliente consultando producto: {}", id);
        ProductWithRecipeDTO dto = productMenuService.getProductById(id);
        return ResponseEntity.ok(dto);
    }

    /**
     * Obtener todas las categorías de productos activas
     * GET /api/menu/categories
     * Acceso: Público (sin autenticación)
     */
    @GetMapping("/menu/categories")
    public ResponseEntity<List<ProductCategory>> getCategories() {
        log.info("Cliente consultando categorías de productos");
        List<ProductCategory> categories = productMenuService.getActiveCategories();
        return ResponseEntity.ok(categories);
    }
}


