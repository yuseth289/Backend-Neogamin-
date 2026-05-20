package com.neogaming.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para el inicio de sesión.
 *
 * Ejemplo de request body:
 * {
 *   "email":    "juan@example.com",
 *   "password": "MiClave123!"
 * }
 *
 * Nota de seguridad: cuando las credenciales son incorrectas, el sistema
 * retorna siempre el mismo mensaje genérico ("Credenciales inválidas") sin
 * indicar si el email o la contraseña son los que fallaron. Esto evita
 * que un atacante pueda enumerar emails registrados.
 */
public record LoginRequest(

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El formato del email no es válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {}
