package co.edu.uniquindio.servly.DTO.Inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa un detalle de receta (Item + cantidad, opcionalidad y anotación).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemDetailDTO {
    private Long id;
    private Integer quantity;
    private String annotation;
    private Boolean isOptional;
    private Long itemId;
    private Long recipeId;
}
