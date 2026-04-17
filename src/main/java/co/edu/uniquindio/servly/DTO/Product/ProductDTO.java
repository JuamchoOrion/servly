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
    private String imageUrl;

    /**
     * Convierte una entidad Product a ProductDTO
     */
    public static ProductDTO fromEntity(Product product) {
        String categoryName = null;
        Long categoryId = null;

        try {
            if (product.getCategory() != null) {
                categoryId = product.getCategory().getId();
                categoryName = product.getCategory().getName();
            }
        } catch (Exception e) {
            // Categoría no encontrada (deletada o eliminada)
            categoryId = null;
            categoryName = null;
        }

        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .description(product.getDescription())
                .active(product.getActive())
                .categoryId(categoryId)
                .categoryName(categoryName)
                .recipeId(product.getRecipe() != null ? product.getRecipe().getId() : null)
                .imageUrl(product.getImageUrl())
                .build();
    }
}
