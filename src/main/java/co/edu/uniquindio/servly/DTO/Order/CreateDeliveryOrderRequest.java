package co.edu.uniquindio.servly.DTO.Order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDeliveryOrderRequest {

    @NotEmpty(message = "La dirección no puede estar vacía")
    private String address;

    @NotEmpty(message = "El teléfono no puede estar vacío")
    private String phoneNumber;

    @NotEmpty(message = "El nombre del cliente no puede estar vacío")
    private String clientName;

    @NotNull(message = "El tiempo de entrega no puede ser nulo")
    private Integer deliveryTime;

    @NotEmpty(message = "La orden debe contener al menos un item")
    private java.util.List<OrderItemVariationDTO> items;
}

