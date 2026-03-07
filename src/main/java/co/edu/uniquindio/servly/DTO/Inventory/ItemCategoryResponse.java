package co.edu.uniquindio.servly.DTO.Inventory;

import co.edu.uniquindio.servly.model.entity.ItemCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response para una categoría de items.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemCategoryResponse {

    private Long id;
    private String name;
    private String description;
    private Boolean active;

    public static ItemCategoryResponse from(ItemCategory category) {
        return ItemCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .active(category.getActive())
                .build();
    }
}

