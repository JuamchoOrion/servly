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

        // ── ORÍGENES LOCALES ──────────────────────────────
        configuration.addAllowedOrigin("http://localhost:4200");
        configuration.addAllowedOrigin("http://127.0.0.1:4200");
        configuration.addAllowedOrigin("http://localhost:3000");
        configuration.addAllowedOrigin("http://127.0.0.1:3000");

        // ── ORÍGENES PRODUCCIÓN — agrega los https ────────
        configuration.addAllowedOrigin("http://56.124.52.198");   // puedes dejarlo
        configuration.addAllowedOrigin("https://56.124.52.198");  // agrega este
        configuration.addAllowedOrigin("https://servlyapp.duckdns.org");  // ← NUEVO principal
        configuration.addAllowedOrigin("http://servlyapp.duckdns.org");   // ← por si acaso

        configuration.addAllowedOrigin(frontendUrl);

        // ── MÉTODOS ───────────────────────────────────────
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // ── HEADERS ───────────────────────────────────────
        configuration.setAllowedHeaders(Arrays.asList("*"));

        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Set-Cookie"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}