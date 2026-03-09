package co.edu.uniquindio.servly.DTO.Inventory;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para crear una categoría de items.
 * Usado en bodega/inventario.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateItemCategoryRequest {

    @NotBlank(message = "El nombre de la categoría es requerido")
    private String name;  // p. ej: "Alimentos", "Limpieza", "Oficina"

    private String description;
}

