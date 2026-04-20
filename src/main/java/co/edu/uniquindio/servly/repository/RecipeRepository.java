package co.edu.uniquindio.servly.repository;

import co.edu.uniquindio.servly.model.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad Recipe.
 * Maneja las operaciones de base de datos para recetas.
 * Una receta vincula productos (menú) con items (inventario).
 */
@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    /**
     * Busca recetas por nombre (eager loading de itemDetailList)
     * Evita LazyInitializationException al acceder a itemDetailList fuera de la transacción
     */
    @Query("SELECT DISTINCT r FROM Recipe r LEFT JOIN FETCH r.itemDetailList WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Recipe> findByNameContainingIgnoreCaseEager(@Param("name") String name);

    /**
     * Obtiene todas las recetas con eager loading de itemDetailList
     */
    @Query("SELECT DISTINCT r FROM Recipe r LEFT JOIN FETCH r.itemDetailList")
    List<Recipe> findAllEager();

    List<Recipe> findByNameContainingIgnoreCase(String name);
}

