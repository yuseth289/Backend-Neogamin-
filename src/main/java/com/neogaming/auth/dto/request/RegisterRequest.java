package com.neogaming.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para el registro de un nuevo usuario.
 *
 * Validaciones aplicadas:
 *  - email     : obligatorio, formato email válido
 *  - password  : obligatorio, mínimo 8 caracteres, máximo 100
 *  - firstName : obligatorio, máximo 100 caracteres
 *  - lastName  : obligatorio, máximo 100 caracteres
 *  - phone     : opcional, máximo 20 caracteres
 *
 * Ejemplo de request body:
 * {
 *   "email":     "juan@example.com",
 *   "password":  "MiClave123!",
 *   "firstName": "Juan",
 *   "lastName":  "García",
 *   "phone":     "3001234567"
 * }
 */
public record RegisterRequest(

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El formato del email no es válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
        String password,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String firstName,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
        String lastName,

        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String phone
) {}
