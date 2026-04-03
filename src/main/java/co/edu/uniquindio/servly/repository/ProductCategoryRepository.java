package co.edu.uniquindio.servly.repository;

import co.edu.uniquindio.servly.model.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    /**
     * Obtiene una categoría por nombre
     */
    Optional<ProductCategory> findByName(String name);

    /**
     * Obtiene todas las categorías activas
     */
    List<ProductCategory> findByActiveTrue();
}

