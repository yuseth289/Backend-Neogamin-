package com.neogaming.config;

import com.neogaming.auth.filter.InternalTokenFilter;
import com.neogaming.auth.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración principal de seguridad para NeoGaming.
 *
 * Define:
 *  - Política de sesiones: STATELESS (sin estado en el servidor, solo JWT)
 *  - Reglas de acceso por endpoint (qué es público y qué requiere autenticación)
 *  - Proveedor de autenticación (DaoAuthenticationProvider con BCrypt)
 *  - Integración del filtro JWT (JwtAuthenticationFilter)
 *  - Desactivación de CSRF (no necesario en APIs REST con JWT)
 *
 * Control fino de roles por endpoint:
 *  Los endpoints de SELLER y ADMIN usan @PreAuthorize en el controlador.
 *  Esta clase solo define la regla base: "todo lo demás requiere autenticación".
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Habilita @PreAuthorize, @PostAuthorize en controladores y servicios
@RequiredArgsConstructor
public class SecurityConfig {

    /** Filtro que valida el JWT en cada request y carga el usuario en el SecurityContext */
    private final JwtAuthenticationFilter jwtAuthFilter;

    /** Filtro que valida X-Internal-Token para endpoints /internal/** */
    private final InternalTokenFilter internalTokenFilter;

    /** Implementación que carga los datos del usuario desde la base de datos */
    private final UserDetailsService userDetailsService;

    @Value("${app.cors.allowed-origins:http://localhost:4000,http://localhost:4200}")
    private String corsAllowedOrigins;

    /**
     * Define la cadena de filtros de seguridad.
     *
     * Orden de los filtros (de primero a último):
     *  1. JwtAuthenticationFilter  → extrae y valida el token JWT
     *  2. UsernamePasswordAuthenticationFilter (reemplazado)
     *  3. Reglas de autorización por path
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Reglas de acceso por endpoint
                .authorizeHttpRequests(auth -> auth

                        // ── Rutas públicas de autenticación ──
                        .requestMatchers("/auth/**").permitAll()

                        // ── Catálogo público (lectura) ──
                        .requestMatchers(HttpMethod.GET, "/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/brands/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/sellers/{storeSlug}").permitAll()

                        // ── Imágenes subidas por vendedores ──
                        .requestMatchers(HttpMethod.GET, "/files/**").permitAll()

                        // ── Webhook de Mercado Pago (firma propia de MP) ──
                        .requestMatchers(HttpMethod.POST, "/webhooks/**").permitAll()

                        // ── Endpoints internos (Python AI service → Spring Boot) ──
                        // Protegidos por InternalTokenFilter (X-Internal-Token), nunca por JWT
                        .requestMatchers("/internal/**").permitAll()

                        // ── Documentación Swagger (solo en dev) ──
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // ── Todo lo demás requiere JWT válido ──
                        .anyRequest().authenticated()
                )

                // Registrar el proveedor de autenticación (BCrypt + UserDetailsService)
                .authenticationProvider(authenticationProvider())

                // InternalTokenFilter (order ~1000) corre antes que JwtAuthenticationFilter (order ~1800)
                // Spring Security 7 requiere que el anchor sea un filtro built-in con orden registrado.
                // LogoutFilter (1100) < UsernamePasswordAuthenticationFilter (1900) garantiza el orden.
                .addFilterBefore(internalTokenFilter, LogoutFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    /**
     * Proveedor de autenticación que:
     *  - Carga el usuario por email desde la base de datos (UserDetailsService)
     *  - Verifica la contraseña usando BCrypt (PasswordEncoder)
     *
     * Se usa en el flujo de login (AuthService lo inyecta vía AuthenticationManager).
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Encoder de contraseñas con BCrypt.
     * Factor de trabajo (strength) = 10 por defecto, balance entre seguridad y rendimiento.
     *
     * Las contraseñas NUNCA se almacenan en texto plano. Solo el hash BCrypt en la BD.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Expone el AuthenticationManager como bean para que AuthService pueda inyectarlo.
     * Se usa para autenticar credenciales en el flujo de login.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(corsAllowedOrigins.split(",")));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        cfg.setAllowCredentials(false);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
