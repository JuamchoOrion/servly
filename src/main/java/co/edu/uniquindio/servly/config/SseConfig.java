package co.edu.uniquindio.servly.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración para soporte de Server-Sent Events (SSE).
 *
 * - Timeout de MVC async = -1 (sin timeout, el SseEmitter gestiona el suyo propio de 4h)
 * - Pool de hilos dedicado para peticiones async (SSE), evitando agotar el pool HTTP principal
 */
@Configuration
public class SseConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(-1); // SseEmitter controla su propio timeout
        configurer.setTaskExecutor(sseTaskExecutor());
    }

    @Bean
    public ThreadPoolTaskExecutor sseTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("sse-");
        executor.initialize();
        return executor;
    }
}
