package co.edu.uniquindio.servly.repository;

import co.edu.uniquindio.servly.model.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    /**
     * Obtiene todos los items activos
     */
    @Query("SELECT i FROM Item i WHERE i.active = true")
    List<Item> findAllActive();

    /**
     * Obtiene un item activo por ID
     */
    @Query("SELECT i FROM Item i WHERE i.id = :id AND i.active = true")
    Optional<Item> findByIdActive(@Param("id") Long id);

    /**
     * Obtiene items por categoría
     */
    @Query("SELECT i FROM Item i WHERE i.itemCategory.id = :categoryId AND i.active = true")
    List<Item> findByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * Busca items por nombre
     */
    @Query("SELECT i FROM Item i WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :name, '%')) AND i.active = true")
    List<Item> findByNameContaining(@Param("name") String name);
}

