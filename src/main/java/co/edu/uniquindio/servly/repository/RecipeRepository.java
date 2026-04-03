package co.edu.uniquindio.servly.repository;

import co.edu.uniquindio.servly.model.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la entidad Recipe.
 * Maneja las operaciones de base de datos para recetas.
 * Una receta vincula productos (menú) con items (inventario).
 */
@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
}

