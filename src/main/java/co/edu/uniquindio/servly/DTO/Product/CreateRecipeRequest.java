package co.edu.uniquindio.servly.DTO.Product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRecipeRequest {

    @NotBlank(message = "El nombre de la receta es requerido")
    private String name;

    private String description;

    @NotEmpty(message = "La receta debe tener al menos un item")
    private List<ItemDetailRequest> itemDetails;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemDetailRequest {
        private Long itemId;
        private Integer quantity;
        private Boolean isOptional;
        private Integer minQuantity;
        private Integer maxQuantity;
    }
}
