package co.edu.uniquindio.servly.DTO.Order;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemVariationDTO {

    private Long productId;
    private Integer quantity;
    private String annotations;
    // Map<itemId, cantidadElegida>
    // Ej: {5: 2, 8: 1} = 2 de queso, 1 de cebolla
    private Map<Long, Integer> itemQuantityOverrides;
}

