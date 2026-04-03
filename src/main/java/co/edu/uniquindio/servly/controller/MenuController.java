package co.edu.uniquindio.servly.controller;

import co.edu.uniquindio.servly.DTO.Product.ProductWithRecipeDTO;
import co.edu.uniquindio.servly.DTO.Product.ItemDetailDTO;
import co.edu.uniquindio.servly.model.entity.Product;
import co.edu.uniquindio.servly.model.entity.Recipe;
import co.edu.uniquindio.servly.model.entity.ItemDetail;
import co.edu.uniquindio.servly.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador público para que CLIENTES obtengan productos con opciones de variación
 */
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
@Slf4j
public class MenuController {

    private final ProductRepository productRepository;

    /**
     * Obtener todos los productos disponibles con sus variaciones
     * GET /api/menu/products
     * Acceso: Público (sin autenticación)
     */
    @GetMapping("/products")
    public ResponseEntity<List<ProductWithRecipeDTO>> getAvailableProducts() {
        log.info("Cliente consultando menú de productos");

        List<Product> products = productRepository.findByActiveTrue();

        List<ProductWithRecipeDTO> dtos = products.stream()
                .map(this::toProductWithRecipeDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Obtener un producto específico con sus variaciones de items
     * GET /api/menu/products/{id}
     * Acceso: Público
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductWithRecipeDTO> getProduct(@PathVariable Long id) {
        log.info("Cliente consultando producto: {}", id);

        Product product = productRepository.findById(id)
                .filter(p -> p.getActive() != null && p.getActive())
                .orElseThrow(() -> new co.edu.uniquindio.servly.exception.NotFoundException(
                        "Producto no encontrado"));

        return ResponseEntity.ok(toProductWithRecipeDTO(product));
    }

    private ProductWithRecipeDTO toProductWithRecipeDTO(Product product) {
        Recipe recipe = product.getRecipe();

        List<ItemDetailDTO> itemDetails = (recipe != null && recipe.getItemDetailList() != null)
            ? recipe.getItemDetailList().stream()
                .map(itemDetail -> ItemDetailDTO.builder()
                        .id(itemDetail.getId())
                        .itemId(itemDetail.getItem().getId())
                        .itemName(itemDetail.getItem().getName())
                        .baseQuantity(itemDetail.getQuantity())
                        .annotation(itemDetail.getAnnotation())
                        .isOptional(itemDetail.getIsOptional())
                        .minQuantity(itemDetail.getMinQuantity())
                        .maxQuantity(itemDetail.getMaxQuantity())
                        .build())
                .collect(Collectors.toList())
            : List.of();

        return ProductWithRecipeDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .basePrice(product.getPrice())
                .description(product.getDescription())
                .recipeItems(itemDetails)
                .build();
    }
}

