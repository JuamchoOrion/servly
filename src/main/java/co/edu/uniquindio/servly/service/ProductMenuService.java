package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Product.ProductWithRecipeDTO;
import co.edu.uniquindio.servly.DTO.Product.ItemDetailDTO;
import co.edu.uniquindio.servly.model.entity.Product;
import co.edu.uniquindio.servly.model.entity.Recipe;
import co.edu.uniquindio.servly.model.entity.ProductCategory;
import co.edu.uniquindio.servly.repository.ProductRepository;
import co.edu.uniquindio.servly.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
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
    private final ProductCategoryRepository productCategoryRepository;

    /**
     * Obtener todos los productos activos con paginación
     */
    @Transactional(readOnly = true)
    public Page<ProductWithRecipeDTO> getActiveProductsPaginated(Pageable pageable) {
        log.info("Obteniendo productos activos con paginación: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Product> products = productRepository.findActiveProductsWithActiveCategories(pageable);
        Page<ProductWithRecipeDTO> dtos = products.map(this::toProductWithRecipeDTO);
        log.info("Retornando {} productos de un total de {}", dtos.getNumberOfElements(), dtos.getTotalElements());
        return dtos;
    }

    /**
     * Obtener todos los productos activos (sin paginación)
     */
    @Transactional(readOnly = true)
    public List<ProductWithRecipeDTO> getActiveProducts() {
        log.info("Obteniendo todos los productos activos");
        return productRepository.findActiveProductsWithActiveCategories()
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
     * Obtener todas las categorías de productos activas
     */
    @Transactional(readOnly = true)
    public List<ProductCategory> getActiveCategories() {
        log.info("Obteniendo categorías de productos activas");
        return productCategoryRepository.findByActiveTrueAndDeletedFalse();
    }

    /**
     * Transformar Product a ProductWithRecipeDTO
     * Incluye los ItemDetails de la receta con sus opciones de variación
     */
    private ProductWithRecipeDTO toProductWithRecipeDTO(Product product) {
        List<ItemDetailDTO> itemDetails = List.of();

        try {
            Recipe recipe = product.getRecipe();

            if (recipe != null && recipe.getItemDetailList() != null && !recipe.getItemDetailList().isEmpty()) {
                try {
                    itemDetails = recipe.getItemDetailList().stream()
                        .map(itemDetail -> {
                            if (itemDetail.getItem() == null) {
                                return null;
                            }
                            return ItemDetailDTO.builder()
                                    .id(itemDetail.getId())
                                    .itemId(itemDetail.getItem().getId())
                                    .itemName(itemDetail.getItem().getName())
                                    .baseQuantity(itemDetail.getQuantity())
                                    .annotation(itemDetail.getAnnotation())
                                    .isOptional(itemDetail.getIsOptional())
                                    .minQuantity(itemDetail.getMinQuantity())
                                    .maxQuantity(itemDetail.getMaxQuantity())
                                    .build();
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                } catch (Exception e) {
                    log.warn("Error procesando ItemDetails para producto {}: {}", product.getId(), e.getMessage());
                    itemDetails = List.of();
                }
            }
        } catch (Exception e) {
            log.warn("Error accediendo a recipe para producto {}: {}", product.getId(), e.getMessage());
            itemDetails = List.of();
        }

        return ProductWithRecipeDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .basePrice(product.getPrice())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .recipeItems(itemDetails)
                .build();
    }
}

