package com.neogaming.auth.provider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Proveedor de tokens JWT para NeoGaming.
 *
 * Responsabilidades:
 *  - Generar access tokens (JWT firmados con HMAC-SHA256)
 *  - Validar y parsear tokens recibidos en los requests
 *
 * Estructura del JWT generado:
 *  Header: { "alg": "HS256" }
 *  Payload: {
 *    "sub":       "uuid-del-usuario",
 *    "role":      "CLIENT" | "SELLER" | "ADMIN",
 *    "email":     "usuario@example.com",
 *    "sessionId": "uuid-de-la-sesion",
 *    "iat":       timestamp-emisión,
 *    "exp":       timestamp-expiración
 *  }
 *
 * El claim "sessionId" permite revocar la sesión en la base de datos
 * sin esperar a que el access token expire naturalmente.
 *
 * Configuración (application.yml):
 *  app.jwt.secret               → Clave HMAC (mínimo 64 caracteres)
 *  app.jwt.access-token-minutes → Duración del access token (default: 15)
 */
@Component
public class JwtTokenProvider {

    /** Clave secreta derivada del secreto configurado en application.yml */
    private final SecretKey secretKey;

    /** Duración en minutos del access token (ej: 15) */
    private final long accessTokenMinutes;

    /**
     * Constructor: deriva la SecretKey desde la cadena de configuración.
     * La clave debe tener al menos 64 caracteres para HMAC-SHA256 de 512 bits.
     */
    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-minutes:15}") long accessTokenMinutes) {
        // Convierte el string secreto en una SecretKey HMAC-SHA
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenMinutes = accessTokenMinutes;
    }

    /**
     * Genera un access token JWT para el usuario y sesión dados.
     *
     * @param userId    UUID del usuario autenticado
     * @param role      Rol del usuario (ej: "CLIENT", "SELLER", "ADMIN")
     * @param email     Email del usuario (informativo, no se usa para autenticar)
     * @param sessionId UUID de la sesión activa (para verificación de revocación)
     * @return Token JWT firmado como cadena de texto
     */
    public String generateAccessToken(UUID userId, String role, String email, UUID sessionId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(userId.toString())          // claim "sub": identificador del usuario
                .claim("role", role)                  // claim personalizado: rol del usuario
                .claim("email", email)                // claim informativo: email
                .claim("sessionId", sessionId.toString()) // claim para revocación de sesión
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Parsea y verifica la firma de un token JWT.
     * Lanza JwtException si el token es inválido, expirado o tiene firma incorrecta.
     *
     * @param token Token JWT en formato String
     * @return Claims (payload) del token si es válido
     * @throws JwtException si el token es inválido o expirado
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)   // Verifica la firma HMAC-SHA256
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Verifica si un token JWT es válido (firma correcta y no expirado).
     * No lanza excepción: retorna false para cualquier token inválido.
     *
     * @param token Token JWT a verificar
     * @return true si el token es válido y no está expirado
     */
    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Token inválido, expirado o con firma incorrecta
            return false;
        }
    }
}
