package com.neogaming.auth.filter;

import com.neogaming.auth.provider.JwtTokenProvider;
import com.neogaming.auth.repository.SessionRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Filtro de autenticación JWT que se ejecuta una sola vez por request HTTP.
 *
 * Flujo de procesamiento en cada request:
 *  1. Extrae el token del header "Authorization: Bearer <token>"
 *  2. Valida la firma y expiración del JWT (JwtTokenProvider)
 *  3. Extrae el sessionId de los claims del token
 *  4. Verifica en la BD que la sesión no esté revocada (SessionRepository)
 *  5. Si todo es válido, carga el usuario en el SecurityContext
 *  6. Continúa con el siguiente filtro en la cadena
 *
 * Si el token falta, es inválido o la sesión está revocada:
 *  - NO se lanza excepción aquí
 *  - Simplemente no se carga nada en el SecurityContext
 *  - Spring Security rechazará el request automáticamente en los endpoints protegidos
 *
 * Los claims del JWT se almacenan en authentication.getDetails() para que
 * SecurityUtils pueda recuperar userId, sessionId y role fácilmente.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Proveedor que valida y parsea el JWT */
    private final JwtTokenProvider jwtTokenProvider;

    /** Repositorio para verificar si la sesión fue revocada (logout) */
    private final SessionRepository sessionRepository;

    /**
     * Lógica principal del filtro. Se ejecuta en cada request HTTP.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Extraer el token del header Authorization
        String token = extractTokenFromRequest(request);

        // 2. Solo procesar si hay token y es válido (firma + expiración)
        if (token != null && jwtTokenProvider.isTokenValid(token)) {

            // 3. Parsear los claims del JWT
            Claims claims = jwtTokenProvider.parseToken(token);
            UUID sessionId = UUID.fromString(claims.get("sessionId", String.class));

            // 4. Verificar que la sesión esté activa (no revocada) en la base de datos
            sessionRepository.findById(sessionId)
                    .filter(session -> session.getRevokedAt() == null) // null = activa
                    .ifPresent(session -> {

                        // 5. Construir el objeto de autenticación
                        String role = claims.get("role", String.class);

                        // El "principal" es el userId (String). El rol se carga como autoridad.
                        // Los claims se guardan en "details" para acceso posterior en SecurityUtils.
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        claims.getSubject(),            // principal = userId (String)
                                        null,                           // credenciales (no necesarias post-auth)
                                        List.of(new SimpleGrantedAuthority("ROLE_" + role)) // autoridades
                                );
                        authentication.setDetails(claims); // claims disponibles via SecurityUtils

                        // 6. Cargar en el SecurityContext (el request queda autenticado)
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }

        // Continuar con el siguiente filtro en la cadena independientemente del resultado
        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el token JWT del header Authorization.
     *
     * Formato esperado: "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
     *
     * @param request Request HTTP entrante
     * @return Token JWT sin el prefijo "Bearer ", o null si no hay token
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        // Verificar que el header existe y comienza con "Bearer "
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7); // Eliminar los 7 caracteres de "Bearer "
        }

        return null; // No hay token en el request
    }
}
