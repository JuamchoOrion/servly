package co.edu.uniquindio.servly.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear una nueva mesa en el restaurante
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRestaurantTableRequest {

    @NotNull(message = "El número de mesa es requerido")
    @Positive(message = "El número de mesa debe ser mayor a 0")
    private Integer tableNumber;

    @NotNull(message = "La capacidad es requerida")
    @Positive(message = "La capacidad debe ser mayor a 0")
    private Integer capacity;

    private String location;
}

