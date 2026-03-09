package co.edu.uniquindio.servly.repository;

import co.edu.uniquindio.servly.model.entity.ItemCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemCategoryRepository extends JpaRepository<ItemCategory, Long> {

    /**
     * Busca una categoría por nombre que no esté eliminada.
     */
    @Query("SELECT ic FROM ItemCategory ic WHERE ic.name = ?1 AND ic.deleted = false")
    Optional<ItemCategory> findByName(String name);

    /**
     * Verifica si existe una categoría con ese nombre que no esté eliminada.
     */
    @Query("SELECT CASE WHEN COUNT(ic) > 0 THEN true ELSE false END FROM ItemCategory ic WHERE ic.name = ?1 AND ic.deleted = false")
    boolean existsByName(String name);

    /**
     * Obtiene todas las categorías que no están eliminadas.
     */
    @Query("SELECT ic FROM ItemCategory ic WHERE ic.deleted = false")
    List<ItemCategory> findAll();

    /**
     * Obtiene todas las categorías que no están eliminadas con paginación.
     */
    @Query("SELECT ic FROM ItemCategory ic WHERE ic.deleted = false")
    Page<ItemCategory> findAllPaginated(Pageable pageable);

    /**
     * Obtiene una categoría por ID que no esté eliminada.
     */
    @Query("SELECT ic FROM ItemCategory ic WHERE ic.id = ?1 AND ic.deleted = false")
    Optional<ItemCategory> findById(Long id);

    /**
     * Obtiene una categoría por ID sin filtro de deleted (para soft delete).
     */
    @Query("SELECT ic FROM ItemCategory ic WHERE ic.id = ?1")
    Optional<ItemCategory> findByIdIncludingDeleted(Long id);
}

