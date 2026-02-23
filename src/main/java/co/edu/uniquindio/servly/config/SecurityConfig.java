package co.edu.uniquindio.servly.config;

import co.edu.uniquindio.servly.handlers.OAuth2AuthenticationFailureHandler;
import co.edu.uniquindio.servly.handlers.OAuth2AuthenticationSuccessHandler;
import co.edu.uniquindio.servly.security.JwtAuthenticationFilter;
import co.edu.uniquindio.servly.security.TableSessionFilter;
import co.edu.uniquindio.servly.service.OAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración principal de Spring Security.
 *
 * Dos tipos de autenticación coexisten:
 *  1. JWT de usuario (staff)   → procesa JwtAuthenticationFilter
 *  2. Token de mesa (cliente)  → procesa TableSessionFilter → ROLE_CLIENTE
 *
 * Rutas:
 *  /api/auth/**       → públicas (login, 2FA, reset)
 *  /api/client/session → pública (escaneo QR)
 *  /api/client/**     → requiere ROLE_CLIENTE (sesión de mesa)
 *  /api/admin/**      → requiere ROLE_ADMIN
 *  /api/staff/**      → requiere rol de staff
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter            jwtAuthFilter;
    private final TableSessionFilter                 tableSessionFilter;
    private final UserDetailsService                 userDetailsService;
    private final OAuth2UserService                  oAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2FailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ── Públicos ─────────────────────────────────────────────────
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/verify-2fa",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/refresh-token",
                                "/api/client/session",       // Escaneo QR
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()

                        // ── Administración ────────────────────────────────────────────
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ── Cliente anónimo (sesión de mesa) ──────────────────────────
                        .requestMatchers("/api/client/**").hasRole("CLIENTE")

                        // ── Staff ─────────────────────────────────────────────────────
                        .requestMatchers("/api/staff/**")
                        .hasAnyRole("ADMIN", "CAJERO", "MESERO", "COCINA", "STOREKEEPER")

                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(e -> e.userService(oAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                .authenticationProvider(authenticationProvider())
                // TableSessionFilter primero (rutas de cliente), luego JWT (rutas de staff)
                .addFilterBefore(tableSessionFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, TableSessionFilter.class)
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}