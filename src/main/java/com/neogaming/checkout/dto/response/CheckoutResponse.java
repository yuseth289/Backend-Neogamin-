package com.neogaming.checkout.dto.response;

import com.neogaming.common.enums.EstadoCheckout;
import com.neogaming.common.enums.TipoPago;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO de salida con los datos del checkout activo.
 *
 * Ejemplo de respuesta JSON:
 * {
 *   "id":            "uuid",
 *   "status":        "PENDING",
 *   "items":         [...],
 *   "subtotal":      350000.00,
 *   "shippingCost":  0.00,
 *   "total":         350000.00,
 *   "paymentMethod": "MP_PSE",
 *   "expiresAt":     "2026-05-19T11:00:00Z",
 *   "minutesLeft":   28
 * }
 */
public record CheckoutResponse(

        UUID id,
        EstadoCheckout status,

        /** Ítems del checkout con precios capturados al momento de iniciarlo */
        List<CheckoutItemResponse> items,

        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal total,

        TipoPago paymentMethod,

        /** Momento exacto en que el checkout expira */
        Instant expiresAt,

        /** Minutos restantes antes de que expire (calculado al responder) */
        long minutesLeft,

        Instant createdAt
) {}
