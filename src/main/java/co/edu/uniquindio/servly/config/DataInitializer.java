package co.edu.uniquindio.servly.config;

import co.edu.uniquindio.servly.model.entity.User;
import co.edu.uniquindio.servly.model.enums.AuthProvider;
import co.edu.uniquindio.servly.model.enums.Role;
import co.edu.uniquindio.servly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Inicializa datos de prueba al iniciar la aplicación.
 * Crea un usuario ADMIN por defecto si no existe.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createAdminUser();
    }

    private void createAdminUser() {
        String adminEmail = "admin@servly.com";
        
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setName("Administrador");
            admin.setRole(Role.ADMIN);
            admin.setProvider(AuthProvider.LOCAL);
            admin.setEnabled(true);
            admin.setAccountNonExpired(true);
            admin.setAccountNonLocked(true);
            admin.setCredentialsNonExpired(true);
            admin.setTwoFactorEnabled(false);
            
            userRepository.save(admin);
            log.info("✅ Usuario ADMIN creado: {} / Contraseña: admin123", adminEmail);
        } else {
            log.info("✅ Usuario ADMIN ya existe: {}", adminEmail);
        }
    }
}
