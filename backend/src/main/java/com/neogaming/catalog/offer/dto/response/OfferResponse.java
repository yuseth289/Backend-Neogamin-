package com.neogaming.catalog.offer.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de salida con los datos de una oferta de descuento.
 *
 * active   → status == ACTIVE (no ha sido eliminada/desactivada explícitamente)
 * vigente  → active && now está dentro del rango startDate–endDate
 */
public record OfferResponse(
        UUID id,
        UUID productId,
        BigDecimal discountPercent,
        Instant startDate,
        Instant endDate,
        boolean active,
        boolean vigente
) {}
