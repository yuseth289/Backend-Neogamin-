package com.neogaming.catalog.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO de entrada para crear o actualizar una categoría.
 * Usado tanto en POST (crear) como en PUT (actualizar) por administradores.
 *
 * Ejemplo de request body (subcategoría):
 * {
 *   "name":        "Teclados Mecánicos",
 *   "description": "Teclados con switches mecánicos individuales",
 *   "parentId":    "uuid-de-perifericos"
 * }
 */
public record CategoryRequest(

        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,

        @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres")
        String description,

        @Size(max = 500, message = "La URL de imagen no puede superar los 500 caracteres")
        String imageUrl,

        /** Nombre del ícono Lucide (ej: lucideGamepad2). Nulo = ícono por defecto. */
        @Size(max = 100, message = "El nombre del ícono no puede superar los 100 caracteres")
        String iconName,

        /** UUID de la categoría padre. null = categoría raíz. */
        UUID parentId
) {}
