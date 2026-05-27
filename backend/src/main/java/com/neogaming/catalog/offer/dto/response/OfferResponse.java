package com.neogaming.catalog.offer.dto.response;

import com.neogaming.common.enums.EstadoGenerico;
import com.neogaming.common.enums.TipoDescuento;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de salida con los datos de una oferta de descuento.
 *
 * Ejemplo de respuesta JSON:
 * {
 *   "id":               "uuid",
 *   "productId":        "uuid",
 *   "name":             "Black Friday 2026",
 *   "discountType":     "PERCENTAGE",
 *   "discountValue":    20.00,
 *   "discountedPrice":  333200.00,
 *   "startDate":        "2026-11-27T00:00:00Z",
 *   "endDate":          "2026-11-30T23:59:59Z",
 *   "vigente":          true,
 *   "status":           "ACTIVE"
 * }
 */
public record OfferResponse(

        UUID id,
        UUID productId,
        String name,
        TipoDescuento discountType,
        BigDecimal discountValue,

        /** Precio final con descuento aplicado en COP */
        BigDecimal discountedPrice,

        Instant startDate,
        Instant endDate,

        /** true si la oferta está activa y dentro del período de vigencia ahora mismo */
        boolean vigente,

        EstadoGenerico status,
        Instant createdAt
) {}
