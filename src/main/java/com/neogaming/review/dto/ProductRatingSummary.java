package com.neogaming.review.dto;

/**
 * Resumen de calificaciones de un producto.
 * Se incluye en la ficha del producto para mostrar la puntuación global.
 */
public record ProductRatingSummary(
        double averageRating,
        long totalReviews
) {}
