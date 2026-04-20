package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Product.CreateProductCategoryRequest;
import co.edu.uniquindio.servly.model.entity.ProductCategory;
import co.edu.uniquindio.servly.repository.ProductCategoryRepository;
import co.edu.uniquindio.servly.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio para gestionar categorías de productos
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductCategoryService {

    private final ProductCategoryRepository productCategoryRepository;

    /**
     * Crear una nueva categoría de productos
     */
    public ProductCategory createCategory(CreateProductCategoryRequest request) {
        if (productCategoryRepository.findByNameAndDeletedFalse(request.getName()).isPresent()) {
            throw new RuntimeException("Ya existe una categoría con el nombre: " + request.getName());
        }

        ProductCategory category = ProductCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(request.getActive() != null ? request.getActive() : true)
                .deleted(false)
                .deletedAt(null)
                .build();

        ProductCategory saved = productCategoryRepository.save(category);
        log.info("Categoría de producto creada: {}", saved.getName());
        return saved;
    }

    /**
     * Obtener categoría por ID - solo no eliminadas
     */
    public ProductCategory getCategoryById(Long id) {
        return productCategoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("Categoría de producto no encontrada con ID: " + id));
    }

    /**
     * Listar todas las categorías activas y no eliminadas
     */
    public List<ProductCategory> getAllActiveCategories() {
        return productCategoryRepository.findByActiveTrueAndDeletedFalse();
    }

    /**
     * Listar todas las categorías no eliminadas
     */
    public List<ProductCategory> getAllCategories() {
        return productCategoryRepository.findByDeletedFalse();
    }

    /**
     * Actualizar categoría
     */
    public ProductCategory updateCategory(Long id, CreateProductCategoryRequest request) {
        ProductCategory category = getCategoryById(id);

        if (!category.getName().equals(request.getName()) &&
            productCategoryRepository.findByNameAndDeletedFalse(request.getName()).isPresent()) {
            throw new RuntimeException("Ya existe una categoría con el nombre: " + request.getName());
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        ProductCategory updated = productCategoryRepository.save(category);
        log.info("Categoría de producto actualizada: {}", updated.getName());
        return updated;
    }

    /**
     * Eliminar categoría (soft delete) - marca como eliminada
     */
    public void deleteCategory(Long id) {
        ProductCategory category = getCategoryById(id);
        category.setDeleted(true);
        category.setDeletedAt(LocalDateTime.now());
        productCategoryRepository.save(category);
        log.info("Categoría de producto eliminada: {}", category.getName());
    }

    /**
     * Restaurar categoría eliminada
     */
    public ProductCategory restoreCategory(Long id) {
        ProductCategory category = productCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoría de producto no encontrada con ID: " + id));
        category.setDeleted(false);
        category.setDeletedAt(null);
        ProductCategory restored = productCategoryRepository.save(category);
        log.info("Categoría de producto restaurada: {}", restored.getName());
        return restored;
    }
}

