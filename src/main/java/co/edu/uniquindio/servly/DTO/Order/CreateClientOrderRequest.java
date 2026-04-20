package co.edu.uniquindio.servly.DTO.Order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClientOrderRequest {

    @NotEmpty(message = "La orden debe contener al menos un producto")
    private List<OrderItemVariationDTO> products;
}

