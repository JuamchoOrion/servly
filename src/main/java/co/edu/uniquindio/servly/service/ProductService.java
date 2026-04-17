package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Product.CreateProductRequest;
import co.edu.uniquindio.servly.model.entity.Product;
import co.edu.uniquindio.servly.model.entity.ProductCategory;
import co.edu.uniquindio.servly.model.entity.Recipe;
import co.edu.uniquindio.servly.repository.ProductRepository;
import co.edu.uniquindio.servly.repository.ProductCategoryRepository;
import co.edu.uniquindio.servly.repository.RecipeRepository;
import co.edu.uniquindio.servly.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final RecipeRepository recipeRepository;

    /**
     * Crear un nuevo producto
     */
    public Product createProduct(CreateProductRequest request) {
        // Validar categoría
        ProductCategory category = productCategoryRepository.findByIdAndDeletedFalse(request.getProductCategoryId())
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));

        // Crear producto
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(category)
                .active(request.getActive() != null ? request.getActive() : true)
                .deleted(false)
                .deletedAt(null)
                .imageUrl(request.getImageUrl())
                .build();

        // Vincular receta si se proporciona
        if (request.getRecipeId() != null) {
            Recipe recipe = recipeRepository.findById(request.getRecipeId())
                    .orElseThrow(() -> new NotFoundException("Receta no encontrada: " + request.getRecipeId()));
            product.setRecipe(recipe);
        }

        return productRepository.save(product);
    }

    /**
     * Crear producto con imagen usando Cloudinary
     */
    public Product createProductWithImage(CreateProductRequest request, MultipartFile image, CloudinaryService cloudinaryService) {
        // Validar categoría
        ProductCategory category = productCategoryRepository.findByIdAndDeletedFalse(request.getProductCategoryId())
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));

        // Crear producto base
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(category)
                .active(request.getActive() != null ? request.getActive() : true)
                .deleted(false)
                .deletedAt(null)
                .build();

        // Vincular receta si se proporciona
        if (request.getRecipeId() != null) {
            Recipe recipe = recipeRepository.findById(request.getRecipeId())
                    .orElseThrow(() -> new NotFoundException("Receta no encontrada: " + request.getRecipeId()));
            product.setRecipe(recipe);
        }

        // Subir imagen a Cloudinary si se proporciona
        if (image != null && !image.isEmpty()) {
            try {
                Map<String, String> uploadResult = cloudinaryService.uploadImage(image, "products");
                product.setImageUrl(uploadResult.get("imageUrl"));
                product.setPublicId(uploadResult.get("publicId"));
                log.info("Imagen cargada para producto: {} - URL: {}", product.getName(), product.getImageUrl());
            } catch (Exception e) {
                log.error("Error al cargar imagen a Cloudinary: {}", e.getMessage());
                // Continuar sin imagen en caso de error
            }
        }

        return productRepository.save(product);
    }

    /**
     * Obtener todos los productos (paginado) - solo no eliminados y sin categorías eliminadas
     */
    public Page<Product> getAllProducts(Pageable pageable) {
        // Usar query que asegura que las categorías no estén eliminadas o sean nulas
        return productRepository.findActiveProductsWithActiveCategories(pageable);
    }

    /**
     * Obtener productos activos (paginado) - solo no eliminados y con categorías válidas
     */
    public Page<Product> getActiveProducts(Pageable pageable) {
        return productRepository.findActiveProductsWithActiveCategories(pageable);
    }

    /**
     * Obtener producto por ID - solo no eliminados
     */
    public Product getProductById(Long id) {
        return productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado: " + id));
    }

    /**
     * Actualizar producto
     */
    public Product updateProduct(Long id, String name, String description, BigDecimal price, Boolean active) {
        Product product = getProductById(id);

        if (name != null && !name.isBlank()) {
            product.setName(name);
        }
        if (description != null) {
            product.setDescription(description);
        }
        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
            product.setPrice(price);
        }
        if (active != null) {
            product.setActive(active);
        }

        return productRepository.save(product);
    }

    /**
     * Eliminar producto (soft delete) - marca como eliminado
     */
    public Product deleteProduct(Long id) {
        Product product = getProductById(id);
        product.setDeleted(true);
        product.setDeletedAt(LocalDateTime.now());

        // Eliminar imagen de Cloudinary si existe
        if (product.getPublicId() != null && !product.getPublicId().isEmpty()) {
            // Necesitamos inyectar CloudinaryService o crearlo aquí
            log.info("Imagen será eliminada de Cloudinary: {}", product.getPublicId());
        }

        return productRepository.save(product);
    }

    /**
     * Restaurar producto eliminado
     */
    public Product restoreProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado: " + id));
        product.setDeleted(false);
        product.setDeletedAt(null);
        return productRepository.save(product);
    }

    /**
     * Activar producto
     */
    public Product activateProduct(Long id) {
        Product product = getProductById(id);
        product.setActive(true);
        return productRepository.save(product);
    }

    /**
     * Desactivar producto
     */
    public Product deactivateProduct(Long id) {
        Product product = getProductById(id);
        product.setActive(false);
        return productRepository.save(product);
    }
}

