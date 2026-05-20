package com.neogaming.user.dto.response;

import com.neogaming.common.enums.EstadoGenerico;
import com.neogaming.common.enums.RolUsuario;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de salida con los datos públicos del perfil de un usuario.
 *
 * Campos excluidos intencionalmente por seguridad:
 *  - passwordHash : nunca se expone al cliente
 *
 * Ejemplo de respuesta JSON:
 * {
 *   "id":            "123e4567-e89b-12d3-a456-426614174000",
 *   "email":         "juan@example.com",
 *   "firstName":     "Juan",
 *   "lastName":      "García",
 *   "phone":         "3001234567",
 *   "avatarUrl":     "https://cdn.neogaming.co/avatars/juan.jpg",
 *   "role":          "CLIENT",
 *   "status":        "ACTIVE",
 *   "emailVerified": false,
 *   "createdAt":     "2026-05-19T10:30:00Z"
 * }
 */
public record UserResponse(

        /** Identificador único del usuario */
        UUID id,

        /** Email de acceso del usuario */
        String email,

        String firstName,
        String lastName,

        /** Teléfono de contacto (puede ser null) */
        String phone,

        /** URL de la imagen de perfil (puede ser null) */
        String avatarUrl,

        /** Rol actual: CLIENT, SELLER o ADMIN */
        RolUsuario role,

        /** Estado de la cuenta: ACTIVE, INACTIVE, SUSPENDED */
        EstadoGenerico status,

        /** true si el usuario verificó su email */
        boolean emailVerified,

        /** Fecha de registro en la plataforma */
        Instant createdAt
) {}
