package com.neogaming.common.util;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Utilidades para acceder al contexto de seguridad del usuario autenticado.
 *
 * El JwtAuthenticationFilter extrae los claims del JWT y los almacena en el
 * SecurityContext al inicio de cada request. Esta clase permite obtener esos
 * datos desde cualquier capa de la aplicación (Service, Controller).
 *
 * Flujo:
 *   Request → JwtAuthenticationFilter → SecurityContext → SecurityUtils (aquí)
 *
 * Importante: estos métodos solo funcionan dentro de un request HTTP autenticado.
 * Si se llaman sin un token válido en el contexto, lanzan IllegalStateException.
 */
public final class SecurityUtils {

    /** Constructor privado: clase de utilidad, no se instancia */
    private SecurityUtils() {}

    /**
     * Retorna el UUID del usuario actualmente autenticado.
     * El UUID se extrae del claim "sub" (subject) del JWT.
     *
     * @return UUID del usuario autenticado
     * @throws IllegalStateException si no hay usuario autenticado en el contexto
     */
    public static UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No hay usuario autenticado en el contexto de seguridad");
        }
        return UUID.fromString((String) auth.getPrincipal());
    }

    /**
     * Retorna el UUID de la sesión activa del usuario.
     * El sessionId se incluye como claim en el JWT para permitir la revocación.
     *
     * @return UUID de la sesión actual
     * @throws IllegalStateException si no hay sesión en los claims del token
     */
    public static UUID getCurrentSessionId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Claims claims) {
            return UUID.fromString(claims.get("sessionId", String.class));
        }
        throw new IllegalStateException("No se encontró sessionId en los claims del token JWT");
    }

    /**
     * Retorna el rol del usuario actualmente autenticado.
     * El rol se incluye como claim "role" en el JWT.
     *
     * @return Nombre del rol (ej: "CLIENT", "SELLER", "ADMIN")
     * @throws IllegalStateException si no hay rol en los claims del token
     */
    public static String getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Claims claims) {
            return claims.get("role", String.class);
        }
        throw new IllegalStateException("No se encontró rol en los claims del token JWT");
    }
}
