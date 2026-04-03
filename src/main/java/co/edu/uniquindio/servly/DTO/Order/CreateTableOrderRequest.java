package co.edu.uniquindio.servly.DTO.Order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTableOrderRequest {

    @NotNull(message = "Número de mesa no puede ser nulo")
    private Integer tableNumber;

    @NotEmpty(message = "La orden debe contener al menos un item")
    private List<OrderItemVariationDTO> items;
}

