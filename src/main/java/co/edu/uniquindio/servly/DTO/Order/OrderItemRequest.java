package co.edu.uniquindio.servly.DTO.Order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {

    @NotNull(message = "ID del producto no puede ser nulo")
    private Long productId;

    @NotNull(message = "Cantidad no puede ser nula")
    @Positive(message = "Cantidad debe ser mayor a 0")
    private Integer quantity;
}

