package co.edu.uniquindio.servly.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        // ORÍGENES PERMITIDOS
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:4200",
                "http://127.0.0.1:4200",
                frontendUrl
        ));

        // MÉTODOS HTTP
        configuration.setAllowedMethods(Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
        ));

        // HEADERS PERMITIDOS
        configuration.setAllowedHeaders(Arrays.asList(
                "*"
        ));

        // HEADERS EXPUESTOS
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Set-Cookie"
        ));

        // PERMITIR COOKIES / AUTH HEADERS
        configuration.setAllowCredentials(true);

        // CACHE PREFLIGHT
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}