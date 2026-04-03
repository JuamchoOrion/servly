package co.edu.uniquindio.servly.repository;

import co.edu.uniquindio.servly.model.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Obtiene todos los productos de una categoría
     */
    List<Product> findByCategory_Id(Long categoryId);

    /**
     * Obtiene productos por nombre (búsqueda)
     */
    List<Product> findByNameContainingIgnoreCase(String name);

    /**
     * Obtiene todos los productos activos para el menú
     */
    List<Product> findByActiveTrue();

    /**
     * Obtiene todos los productos activos (paginado)
     */
    Page<Product> findByActiveTrue(Pageable pageable);
}
