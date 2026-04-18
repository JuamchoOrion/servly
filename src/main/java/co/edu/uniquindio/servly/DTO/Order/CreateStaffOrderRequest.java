package co.edu.uniquindio.servly.DTO.Order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * DTO para crear órdenes desde el mesero (STAFF)
 * El mesero selecciona una mesa y agrega productos
 *
 * Ejemplo:
 * {
 *   "tableNumber": 5,
 *   "products": [
 *     {
 *       "productId": 1,
 *       "quantity": 2,
 *       "itemQuantityOverrides": {"3": 2, "5": 1}
 *     }
 *   ],
 *   "notes": "Sin queso en la hamburguesa"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStaffOrderRequest {

    @NotNull(message = "El número de mesa es requerido")
    @Min(value = 1, message = "El número de mesa debe ser mayor a 0")
    private Integer tableNumber;

    @NotEmpty(message = "La orden debe contener al menos un producto")
    private List<OrderItemVariationDTO> products;

    private String notes; // Notas especiales de la orden (ej: "Sin queso", "Muy picante")
}

