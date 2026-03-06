package co.edu.uniquindio.servly.DTO.Roles;

import co.edu.uniquindio.servly.model.entity.User;
import co.edu.uniquindio.servly.model.enums.AuthProvider;
import co.edu.uniquindio.servly.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Datos de un usuario visibles para el ADMIN.
 * No expone contraseña ni datos internos de seguridad.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String        id;
    private String        name;
    private String        email;
    private Role          role;
    private AuthProvider  provider;
    private boolean       enabled;
    private boolean       twoFactorEnabled;
    private LocalDateTime createdAt;
    private boolean       mustChangePassword;
    private boolean       firstLoginCompleted;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .provider(user.getProvider())
                .enabled(user.isEnabled())
                .twoFactorEnabled(user.isTwoFactorEnabled())
                .createdAt(user.getCreatedAt())
                .mustChangePassword(user.isMustChangePassword())
                .firstLoginCompleted(user.isFirstLoginCompleted())
                .build();
    }
}