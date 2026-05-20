package com.neogaming.review.dto;

import com.neogaming.common.enums.EstadoResena;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de salida con los datos de una reseña.
 * El campo buyerName muestra el nombre del comprador para contexto público.
 */
public record ReviewResponse(
        UUID id,
        UUID productId,
        UUID userId,
        String buyerName,
        int rating,
        String title,
        String body,
        EstadoResena status,
        String rejectReason,
        Instant createdAt
) {}
