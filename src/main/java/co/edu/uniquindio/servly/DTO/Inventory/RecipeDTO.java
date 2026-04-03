package co.edu.uniquindio.servly.DTO.Inventory;

import co.edu.uniquindio.servly.model.entity.Recipe;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO para representar una receta y sus detalles.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeDTO {
    private Long id;
    private String name;
    private Integer quantity;
    private String description;
    private List<ItemDetailDTO> itemDetailList;

    /**
     * Convierte una entidad Recipe a DTO
     */
    public static RecipeDTO fromEntity(Recipe recipe) {
        if (recipe == null) {
            return null;
        }

        List<ItemDetailDTO> itemDetails = recipe.getItemDetailList() != null
                ? recipe.getItemDetailList().stream()
                    .map(ItemDetailDTO::fromEntity)
                    .collect(Collectors.toList())
                : Collections.emptyList();

        return RecipeDTO.builder()
                .id(recipe.getId())
                .name(recipe.getName())
                .quantity(recipe.getQuantity())
                .description(recipe.getDescription())
                .itemDetailList(itemDetails)
                .build();
    }
}

