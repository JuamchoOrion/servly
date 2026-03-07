package co.edu.uniquindio.servly.DTO.Inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request para crear una receta con sus item details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeCreateRequest {
    private String name;
    private Integer quantity;
    private String description;
    private List<ItemDetailCreateRequest> itemDetails;
}
