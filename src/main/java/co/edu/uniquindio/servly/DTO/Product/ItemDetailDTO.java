package co.edu.uniquindio.servly.DTO.Product;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemDetailDTO {

    private Long id;
    private Long itemId;
    private String itemName;
    private Integer baseQuantity;
    private String annotation;
    private Boolean isOptional;
    private Integer minQuantity; // 0 si es opcional
    private Integer maxQuantity; // Máximo que puede seleccionar
}

