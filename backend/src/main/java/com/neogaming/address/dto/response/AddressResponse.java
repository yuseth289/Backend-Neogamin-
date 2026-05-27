package com.neogaming.address.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de salida con los datos de una dirección del usuario.
 *
 * Ejemplo de respuesta JSON:
 * {
 *   "id":         "123e4567-e89b-12d3-a456-426614174000",
 *   "label":      "Casa",
 *   "street":     "Cra 7",
 *   "number":     "# 32-15",
 *   "floor":      "3",
 *   "apartment":  "301",
 *   "city":       "Bogotá",
 *   "department": "Cundinamarca",
 *   "country":    "Colombia",
 *   "postalCode": "110311",
 *   "primary":    true,
 *   "createdAt":  "2026-05-19T10:30:00Z"
 * }
 */
public record AddressResponse(

        /** Identificador único de la dirección */
        UUID id,

        /** Etiqueta de la dirección (ej: "Casa", "Trabajo") */
        String label,

        String street,
        String number,

        /** Piso. Puede ser null si no aplica. */
        String floor,

        /** Apartamento. Puede ser null si no aplica. */
        String apartment,

        String city,
        String department,
        String country,

        /** Código postal colombiano. Puede ser null. */
        String postalCode,

        /** true si es la dirección principal del usuario */
        boolean primary,

        /** Fecha de creación de la dirección */
        Instant createdAt
) {}
