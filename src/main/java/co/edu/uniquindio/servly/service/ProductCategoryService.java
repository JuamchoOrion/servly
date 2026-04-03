package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Product.CreateProductCategoryRequest;
import co.edu.uniquindio.servly.model.entity.ProductCategory;
import co.edu.uniquindio.servly.repository.ProductCategoryRepository;
import co.edu.uniquindio.servly.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        if (productCategoryRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("Ya existe una categoría con el nombre: " + request.getName());
        }

        ProductCategory category = ProductCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        ProductCategory saved = productCategoryRepository.save(category);
        log.info("Categoría de producto creada: {}", saved.getName());
        return saved;
    }

    /**
     * Obtener categoría por ID
     */
    public ProductCategory getCategoryById(Long id) {
        return productCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoría de producto no encontrada con ID: " + id));
    }

    /**
     * Listar todas las categorías activas
     */
    public List<ProductCategory> getAllActiveCategories() {
        return productCategoryRepository.findByActiveTrue();
    }

    /**
     * Listar todas las categorías
     */
    public List<ProductCategory> getAllCategories() {
        return productCategoryRepository.findAll();
    }

    /**
     * Actualizar categoría
     */
    public ProductCategory updateCategory(Long id, CreateProductCategoryRequest request) {
        ProductCategory category = getCategoryById(id);

        if (!category.getName().equals(request.getName()) &&
            productCategoryRepository.findByName(request.getName()).isPresent()) {
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
     * Eliminar categoría (lógico)
     */
    public void deleteCategory(Long id) {
        ProductCategory category = getCategoryById(id);
        productCategoryRepository.delete(category);
        log.info("Categoría de producto eliminada: {}", category.getName());
    }
}

