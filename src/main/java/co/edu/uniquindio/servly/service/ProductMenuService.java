package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Product.ProductWithRecipeDTO;
import co.edu.uniquindio.servly.DTO.Product.ItemDetailDTO;
import co.edu.uniquindio.servly.model.entity.Product;
import co.edu.uniquindio.servly.model.entity.Recipe;
import co.edu.uniquindio.servly.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión del menú digital (productos con variaciones)
 * Centraliza la lógica de obtención de productos y transformación a DTOs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductMenuService {

    private final ProductRepository productRepository;

    /**
     * Obtener todos los productos activos con paginación
     */
    @Transactional(readOnly = true)
    public Page<ProductWithRecipeDTO> getActiveProductsPaginated(Pageable pageable) {
        log.info("Obteniendo productos activos con paginación: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        List<Product> products = productRepository.findByActiveTrue();
        List<ProductWithRecipeDTO> dtos = products.stream()
                .map(this::toProductWithRecipeDTO)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), dtos.size());
        List<ProductWithRecipeDTO> pageContent = dtos.subList(start, end);

        Page<ProductWithRecipeDTO> page = new PageImpl<>(pageContent, pageable, dtos.size());
        log.info("Retornando {} productos de un total de {}", pageContent.size(), dtos.size());
        return page;
    }

    /**
     * Obtener todos los productos activos (sin paginación)
     */
    @Transactional(readOnly = true)
    public List<ProductWithRecipeDTO> getActiveProducts() {
        log.info("Obteniendo todos los productos activos");
        return productRepository.findByActiveTrue()
                .stream()
                .map(this::toProductWithRecipeDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtener un producto específico por ID
     * Solo si está activo
     */
    @Transactional(readOnly = true)
    public ProductWithRecipeDTO getProductById(Long id) {
        log.info("Obteniendo producto activo con ID: {}", id);
        Product product = productRepository.findById(id)
                .filter(p -> p.getActive() != null && p.getActive())
                .orElseThrow(() -> new co.edu.uniquindio.servly.exception.NotFoundException(
                        "Producto no encontrado o no está disponible"));
        return toProductWithRecipeDTO(product);
    }

    /**
     * Transformar Product a ProductWithRecipeDTO
     * Incluye los ItemDetails de la receta con sus opciones de variación
     */
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

