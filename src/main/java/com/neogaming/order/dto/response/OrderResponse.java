package com.neogaming.order.dto.response;

import com.neogaming.common.enums.EstadoPedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO de salida con los datos completos de una orden de compra.
 *
 * Ejemplo de respuesta JSON:
 * {
 *   "id":        "uuid",
 *   "status":    "PAYMENT_APPROVED",
 *   "subtotal":  350000.00,
 *   "total":     350000.00,
 *   "groups": [
 *     {
 *       "sellerId": "uuid",
 *       "status":   "PREPARING",
 *       "items": [...]
 *     }
 *   ]
 * }
 */
public record OrderResponse(

        UUID id,
        EstadoPedido status,

        /** Grupos de ítems organizados por vendedor */
        List<OrderGroupResponse> groups,

        /** Dirección de entrega como fue capturada al momento de la compra */
        Object shippingAddress,

        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal total,

        Instant createdAt,
        Instant updatedAt
) {}
