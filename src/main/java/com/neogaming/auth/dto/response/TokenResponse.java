package com.neogaming.auth.dto.response;

import java.util.UUID;

/**
 * DTO de respuesta para los endpoints de autenticación (login, registro, refresh).
 *
 * Contiene el par de tokens que el cliente debe almacenar para autenticarse:
 *
 *  - accessToken  : JWT de corta duración (15 min). Se envía en cada request
 *                   como "Authorization: Bearer <accessToken>"
 *
 *  - refreshToken : UUID de larga duración (30 días). Se usa SOLO para renovar
 *                   el access token cuando expira (POST /auth/refresh).
 *                   Debe almacenarse de forma segura (no en localStorage en web).
 *
 * Ejemplo de respuesta:
 * {
 *   "accessToken":  "eyJhbGciOiJIUzI1NiJ9...",
 *   "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
 *   "userId":       "123e4567-e89b-12d3-a456-426614174000",
 *   "role":         "CLIENT"
 * }
 */
public record TokenResponse(

        /** JWT para autenticar cada request. Expira en 15 minutos. */
        String accessToken,

        /** Token para renovar el accessToken. Expira en 30 días. */
        String refreshToken,

        /** UUID del usuario autenticado */
        UUID userId,

        /** Rol del usuario: CLIENT, SELLER o ADMIN */
        String role
) {}
