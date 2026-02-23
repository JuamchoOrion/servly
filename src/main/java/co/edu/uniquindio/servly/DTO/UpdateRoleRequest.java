package co.edu.uniquindio.servly.DTO;

import co.edu.uniquindio.servly.model.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRoleRequest {

    @NotNull(message = "El rol es obligatorio")
    private Role role;
}