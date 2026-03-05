package co.edu.uniquindio.servly.config;

import co.edu.uniquindio.servly.handlers.OAuth2AuthenticationFailureHandler;
import co.edu.uniquindio.servly.handlers.OAuth2AuthenticationSuccessHandler;
import co.edu.uniquindio.servly.security.JwtAuthenticationFilter;
import co.edu.uniquindio.servly.security.TableSessionFilter;
import co.edu.uniquindio.servly.service.OAuth2UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
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

import java.util.Map;

/**
 * Configuración principal de Spring Security.
 *
 * Dos tipos de autenticación coexisten:
 *  1. JWT de usuario (staff)   → procesa JwtAuthenticationFilter
 *  2. Token de mesa (cliente)  → procesa TableSessionFilter → ROLE_CLIENT
 *
 * Rutas:
 *  /api/auth/**       → públicas (login, 2FA, reset)
 *  /api/client/session → pública (escaneo QR)
 *  /api/client/**     → requiere ROLE_CLIENT (sesión de mesa)
 *  /api/admin/**      → requiere ROLE_ADMIN
 *  /api/staff/**      → requiere rol de staff
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter            jwtAuthFilter;
    private final TableSessionFilter                 tableSessionFilter;
    private final UserDetailsService                 userDetailsService;
    private final OAuth2UserService                  oAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2FailureHandler;
    private final org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // ✅ HABILITAR CORS - PRIMERO EN LA CADENA
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            String uri = request.getRequestURI();
                            // No loguear accesos no autorizados a /error (evita spam de logs)
                            if (!"/error".equals(uri)) {
                                log.warn("Acceso no autorizado a {}: {}", uri, authException.getMessage());
                            }
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            try {
                                String body = objectMapper.writeValueAsString(Map.of(
                                        "error", "Unauthorized",
                                        "message", "Se requiere autenticación para acceder a este recurso"
                                ));
                                response.getWriter().write(body);
                            } catch (Exception e) {
                                log.error("Error al escribir respuesta JSON: {}", e.getMessage());
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("Acceso denegado a {}: {}", request.getRequestURI(), accessDeniedException.getMessage());
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            try {
                                String body = objectMapper.writeValueAsString(Map.of(
                                        "error", "Forbidden",
                                        "message", "No tiene permisos para acceder a este recurso"
                                ));
                                response.getWriter().write(body);
                            } catch (Exception e) {
                                log.error("Error al escribir respuesta JSON: {}", e.getMessage());
                            }
                        })
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth

                        // ── Públicos ─────────────────────────────────────────────────
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/verify-2fa",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/refresh-token",
                                "/api/auth/logout",
                                "/api/client/session",       // Escaneo QR
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()

                        // ── Endpoints protegidos de autenticación ───────────────────
                        .requestMatchers(
                                "/api/auth/force-password-change",
                                "/api/auth/me"
                        ).authenticated()

                        // ── Administración ────────────────────────────────────────────
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ── Cliente anónimo (sesión de mesa) ──────────────────────────
                        .requestMatchers("/api/client/**").hasRole("CLIENT")

                        // ── Staff ─────────────────────────────────────────────────────
                        .requestMatchers("/api/staff/**")
                        .hasAnyRole("ADMIN", "CASHIER", "WAITER", "KITCHEN", "STOREKEEPER")

                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(e -> e.userService(oAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                        .authorizationEndpoint(auth -> auth
                                .baseUri("/oauth2/authorize")
                        )
                        .redirectionEndpoint(redir -> redir
                                .baseUri("/login/oauth2/code/*")
                        )
                )
                .authenticationProvider(authenticationProvider())
                // TableSessionFilter primero (rutas de cliente), luego JWT (rutas de staff)
                .addFilterBefore(tableSessionFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, TableSessionFilter.class)
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
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

    /**
     * RestTemplate bean para hacer solicitudes HTTP.
     * Usado por RecaptchaService para verificar tokens con Google.
     */
    @Bean
    public org.springframework.web.client.RestTemplate restTemplate() {
        return new org.springframework.web.client.RestTemplate();
    }
}