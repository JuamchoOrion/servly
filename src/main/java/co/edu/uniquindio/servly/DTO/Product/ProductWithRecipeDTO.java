package co.edu.uniquindio.servly.DTO.Product;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductWithRecipeDTO {

    private Long id;
    private String name;
    private BigDecimal basePrice;
    private String description;
    private List<ItemDetailDTO> recipeItems; // Items que componen el producto
}

