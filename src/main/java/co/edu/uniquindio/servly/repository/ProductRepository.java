package co.edu.uniquindio.servly.repository;

import co.edu.uniquindio.servly.model.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Obtiene todos los productos de una categoría que no han sido eliminados
     */
    List<Product> findByCategory_IdAndDeletedFalse(Long categoryId);

    /**
     * Obtiene productos por nombre que no han sido eliminados
     */
    List<Product> findByNameContainingIgnoreCaseAndDeletedFalse(String name);

    /**
     * Obtiene todos los productos activos y no eliminados
     */
    List<Product> findByActiveTrueAndDeletedFalse();

    /**
     * Obtiene todos los productos activos y no eliminados (paginado)
     */
    Page<Product> findByActiveTrueAndDeletedFalse(Pageable pageable);

    /**
     * Obtiene todos los productos no eliminados de una categoría
     */
    List<Product> findByCategoryIdAndDeletedFalse(Long categoryId);

    /**
     * Obtiene producto por ID que no ha sido eliminado
     */
    java.util.Optional<Product> findByIdAndDeletedFalse(Long id);

    /**
     * Obtiene todos los productos no eliminados (paginado)
     */
    Page<Product> findByDeletedFalse(Pageable pageable);

    /**
     * Obtiene productos activos con categorías no eliminadas
     */
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.deleted = false AND p.category.deleted = false")
    List<Product> findActiveProductsWithActiveCategories();

    /**
     * Obtiene productos activos con categorías no eliminadas (paginado)
     */
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.deleted = false AND p.category.deleted = false")
    Page<Product> findActiveProductsWithActiveCategories(Pageable pageable);
}


