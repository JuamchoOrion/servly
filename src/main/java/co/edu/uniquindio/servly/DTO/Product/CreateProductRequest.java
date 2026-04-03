package co.edu.uniquindio.servly.DTO.Product;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProductRequest {

    @NotBlank(message = "El nombre del producto es requerido")
    private String name;

    private String description;

    @NotNull(message = "El precio es requerido")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private BigDecimal price;

    @NotNull(message = "La categoría es requerida")
    private Long productCategoryId;

    @Builder.Default
    private Boolean active = true;

    // Campo opcional para vincular una receta al producto
    // Si se proporciona, el producto usará esa receta para descontar inventario
    private Long recipeId;
}

