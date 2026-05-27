package com.neogaming.review.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

/**
 * Solicitud de creación de reseña por parte del administrador.
 * No requiere orden de compra — la reseña se crea directamente como APPROVED.
 */
public record AdminCreateReviewRequest(

        @NotNull(message = "El ID del producto es obligatorio")
        UUID productId,

        @NotBlank(message = "El nombre del comprador es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String buyerName,

        @Min(value = 1, message = "La calificación mínima es 1 estrella")
        @Max(value = 5, message = "La calificación máxima es 5 estrellas")
        int rating,

        @Size(max = 150, message = "El título no puede superar 150 caracteres")
        String title,

        @Size(max = 5000, message = "El contenido no puede superar 5000 caracteres")
        String body
) {}
