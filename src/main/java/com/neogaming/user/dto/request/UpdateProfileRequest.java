package com.neogaming.user.dto.request;

import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para actualizar el perfil del usuario autenticado.
 *
 * Todos los campos son opcionales: solo se actualizan los que lleguen con valor.
 * Si un campo llega como null, se conserva el valor actual en la base de datos.
 *
 * Ejemplo de request body (actualizar solo el teléfono):
 * {
 *   "phone": "3109876543"
 * }
 */
public record UpdateProfileRequest(

        @Size(min = 1, max = 100, message = "El nombre debe tener entre 1 y 100 caracteres")
        String firstName,

        @Size(min = 1, max = 100, message = "El apellido debe tener entre 1 y 100 caracteres")
        String lastName,

        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String phone,

        @Size(max = 500, message = "La URL del avatar no puede superar los 500 caracteres")
        String avatarUrl
) {}
