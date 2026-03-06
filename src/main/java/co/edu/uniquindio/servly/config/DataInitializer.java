package co.edu.uniquindio.servly.config;

import co.edu.uniquindio.servly.model.entity.User;
import co.edu.uniquindio.servly.model.enums.AuthProvider;
import co.edu.uniquindio.servly.model.enums.Role;
import co.edu.uniquindio.servly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Inicializa el usuario ADMIN al arrancar la aplicación.
 * Las credenciales se leen desde application.properties.
 *
 * Para cambiar el admin:
 *  1. DELETE FROM users WHERE role = 'ADMIN'; (en Supabase)
 *  2. Cambia los valores en application.properties
 *  3. Reinicia la aplicación
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.name}")
    private String adminName;

    @Override
    public void run(String... args) {
        createAdminUser();
    }

    private void createAdminUser() {
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = User.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .name(adminName)
                    .role(Role.ADMIN)
                    .provider(AuthProvider.LOCAL)
                    .enabled(true)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .twoFactorEnabled(false)
                    .mustChangePassword(false)
                    .firstLoginCompleted(true)
                    .build();

            userRepository.save(admin);
            log.info("✅ Usuario ADMIN creado: {}", adminEmail);
        } else {
            log.info("✅ Usuario ADMIN ya existe: {}", adminEmail);
        }
    }
}