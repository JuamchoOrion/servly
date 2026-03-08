package co.edu.uniquindio.servly.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Configuración CORS para permitir peticiones desde el frontend.
 *
 * CORS (Cross-Origin Resource Sharing) permite que aplicaciones en diferentes
 * dominios se comuniquen entre sí.
 *
 * En este caso:
 * - Backend: http://localhost:8081
 * - Frontend: http://localhost:4200
 *
 * Son orígenes diferentes, por eso necesitamos CORS configurado.
 */
@Configuration
public class CorsConfig {

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ ORÍGENES PERMITIDOS
        // Agregar los dominios desde los que se permitirán peticiones
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:4200",      // Angular development
                "http://localhost:3000",      // React/other development
                "http://127.0.0.1:4200",      // Localhost alternativo
                "http://127.0.0.1:3000",      // Localhost alternativo
                frontendUrl                   // Variable de configuración
        ));

        // ✅ MÉTODOS HTTP PERMITIDOS
        configuration.setAllowedMethods(Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"  // Importante para preflight requests
        ));

        // ✅ HEADERS PERMITIDOS EN LAS PETICIONES
        // CRÍTICO: Cookie debe estar permitida para enviar cookies cross-origin
        configuration.setAllowedHeaders(Arrays.asList(
                "Content-Type",
                "Authorization",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "Cookie"  // ✅ CRÍTICO: Permitir header Cookie

        ));

        // ✅ HEADERS EXPUESTOS EN LA RESPUESTA
        // El frontend puede leer estos headers de la respuesta
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Total-Count",
                "X-Page-Count",
                "Set-Cookie"  // ✅ Importante para que el frontend vea las cookies
        ));

        // ✅ PERMITIR CREDENCIALES (cookies, headers de autorización)
        // CRÍTICO: Debe ser true para enviar cookies cross-origin
        configuration.setAllowCredentials(true);

        // ✅ TIEMPO DE CACHÉ PARA PREFLIGHT REQUESTS (en segundos)
        // Los navegadores cachearán la respuesta OPTIONS durante este tiempo
        configuration.setMaxAge(3600L);  // 1 hora

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}

