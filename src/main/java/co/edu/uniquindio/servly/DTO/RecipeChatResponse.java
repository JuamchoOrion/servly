package co.edu.uniquindio.servly.DTO;

import co.edu.uniquindio.servly.model.entity.Recipe;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * DTO simple para respuestas de chat relacionadas con recetas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeChatResponse {
    private Long id;
    private String name;
    private String description;
    private Integer quantity;

    // Nuevo: si la receta está disponible (no faltan ingredientes obligatorios)
    private boolean disponible;

    // Nuevo: lista de nombres de ingredientes obligatorios que no están disponibles
    private List<String> ingredientesNoDisponibles;

    public RecipeChatResponse(Recipe recipe) {
        if (recipe != null) {
            this.id = recipe.getId();
            this.name = recipe.getName();
            this.description = recipe.getDescription();
            this.quantity = recipe.getQuantity();

            List<String> missing = (recipe.getItemDetailList() != null)
                    ? recipe.getItemDetailList().stream()
                        .filter(item -> !Boolean.TRUE.equals(item.getIsOptional()) &&
                                (item.getQuantity() == null || item.getQuantity() == 0))
                        .map(item -> item.getItem() != null ? item.getItem().getName() : null)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
                    : Collections.emptyList();

            this.ingredientesNoDisponibles = missing;
            this.disponible = missing.isEmpty();
        }
    }
}
