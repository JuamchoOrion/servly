package co.edu.uniquindio.servly.DTO.Inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para crear un ItemDetail dentro de una receta.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemDetailCreateRequest {
    private Integer quantity;
    private String annotation;
    private Boolean isOptional;
    private Long itemId;
}
