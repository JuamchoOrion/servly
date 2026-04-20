package co.edu.uniquindio.servly.DTO.Inventory;

import co.edu.uniquindio.servly.model.entity.ItemDetail;
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

    /**
     * Convierte una entidad ItemDetail a DTO
     */
    public static ItemDetailDTO fromEntity(ItemDetail itemDetail) {
        if (itemDetail == null) {
            return null;
        }

        return ItemDetailDTO.builder()
                .id(itemDetail.getId())
                .quantity(itemDetail.getQuantity())
                .annotation(itemDetail.getAnnotation())
                .isOptional(itemDetail.getIsOptional())
                .itemId(itemDetail.getItem() != null ? itemDetail.getItem().getId() : null)
                .recipeId(itemDetail.getRecipe() != null ? itemDetail.getRecipe().getId() : null)
                .build();
    }
}

