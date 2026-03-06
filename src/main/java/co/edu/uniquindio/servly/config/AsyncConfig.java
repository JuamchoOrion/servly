package co.edu.uniquindio.servly.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita @Async (envío de emails sin bloquear el hilo HTTP)
 * y @Scheduled (limpieza periódica de códigos y sesiones expiradas).
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}