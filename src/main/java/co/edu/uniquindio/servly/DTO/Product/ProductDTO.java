package co.edu.uniquindio.servly.DTO.Product;

import co.edu.uniquindio.servly.model.entity.Product;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {

    private Long id;
    private String name;
    private BigDecimal price;
    private String description;
    private Boolean active;
    private Long categoryId;
    private String categoryName;
    private Long recipeId;

    /**
     * Convierte una entidad Product a ProductDTO
     */
    public static ProductDTO fromEntity(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .description(product.getDescription())
                .active(product.getActive())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .recipeId(product.getRecipe() != null ? product.getRecipe().getId() : null)
                .build();
    }
}
