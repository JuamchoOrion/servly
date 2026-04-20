package co.edu.uniquindio.servly.DTO.Order;

import co.edu.uniquindio.servly.model.enums.OrderTableState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {

    @NotNull(message = "Estado no puede ser nulo")
    private OrderTableState status;

    private String notes;
}

