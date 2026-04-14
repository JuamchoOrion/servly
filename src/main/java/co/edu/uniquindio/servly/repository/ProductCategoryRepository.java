package co.edu.uniquindio.servly.repository;

import co.edu.uniquindio.servly.model.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    /**
     * Obtiene una categoría por nombre que no ha sido eliminada
     */
    Optional<ProductCategory> findByNameAndDeletedFalse(String name);

    /**
     * Obtiene todas las categorías activas y no eliminadas
     */
    List<ProductCategory> findByActiveTrueAndDeletedFalse();

    /**
     * Obtiene todas las categorías no eliminadas
     */
    List<ProductCategory> findByDeletedFalse();

    /**
     * Obtiene categoría por ID que no ha sido eliminada
     */
    Optional<ProductCategory> findByIdAndDeletedFalse(Long id);
}

