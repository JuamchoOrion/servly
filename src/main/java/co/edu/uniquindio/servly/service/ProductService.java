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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final RecipeRepository recipeRepository;

    /**
     * Crear un nuevo producto
     */
    public Product createProduct(CreateProductRequest request) {
        // Validar categoría
        ProductCategory category = productCategoryRepository.findById(request.getProductCategoryId())
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));

        // Crear producto
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(category)
                .active(request.getActive() != null ? request.getActive() : true)
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
     * Obtener todos los productos (paginado)
     */
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    /**
     * Obtener productos activos (paginado)
     */
    public Page<Product> getActiveProducts(Pageable pageable) {
        return productRepository.findByActiveTrue(pageable);
    }

    /**
     * Obtener producto por ID
     */
    public Product getProductById(Long id) {
        return productRepository.findById(id)
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
     * Desactivar producto (borrado lógico)
     */
    public Product deleteProduct(Long id) {
        Product product = getProductById(id);
        product.setActive(false);
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

