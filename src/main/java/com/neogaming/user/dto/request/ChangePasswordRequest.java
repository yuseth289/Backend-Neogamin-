package com.neogaming.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para cambiar la contraseña del usuario autenticado.
 *
 * Se requiere la contraseña actual como medida de seguridad adicional,
 * incluso cuando el usuario ya está autenticado con JWT.
 * La confirmación evita errores de escritura en la nueva contraseña.
 *
 * Ejemplo de request body:
 * {
 *   "currentPassword": "MiClaveActual123!",
 *   "newPassword":     "MiNuevaClave456!",
 *   "confirmPassword": "MiNuevaClave456!"
 * }
 */
public record ChangePasswordRequest(

        @NotBlank(message = "La contraseña actual es obligatoria")
        String currentPassword,

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, max = 100, message = "La nueva contraseña debe tener entre 8 y 100 caracteres")
        String newPassword,

        @NotBlank(message = "La confirmación de contraseña es obligatoria")
        String confirmPassword
) {}
