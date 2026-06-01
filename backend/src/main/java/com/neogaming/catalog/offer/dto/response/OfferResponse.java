package com.neogaming.catalog.offer.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de salida con los datos de una oferta de descuento.
 *
 * Ejemplo de respuesta JSON:
 * {
 *   "id":              "uuid",
 *   "productId":       "uuid",
 *   "discountPercent": 20.00,
 *   "startDate":       "2026-11-27T00:00:00Z",
 *   "endDate":         "2026-11-30T23:59:59Z",
 *   "active":          true
 * }
 */
public record OfferResponse(
        UUID id,
        UUID productId,
        BigDecimal discountPercent,
        Instant startDate,
        Instant endDate,
        boolean active
) {}
