package com.neogaming.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para la renovación del access token mediante refresh token.
 *
 * Flujo de uso:
 *  1. El access token expira (15 minutos)
 *  2. El cliente envía el refreshToken que recibió al hacer login/registro
 *  3. El sistema invalida el refresh token anterior y emite un nuevo par de tokens
 *  4. El cliente actualiza sus tokens almacenados
 *
 * Ejemplo de request body:
 * {
 *   "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
 * }
 */
public record RefreshTokenRequest(

        @NotBlank(message = "El refresh token es obligatorio")
        String refreshToken
) {}
