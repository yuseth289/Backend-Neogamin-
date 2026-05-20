package com.neogaming.order.dto.response;

import com.neogaming.common.enums.EstadoPedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de salida resumido para listados de órdenes del usuario.
 * No incluye el detalle de ítems — solo los datos para la tarjeta de orden.
 */
public record OrderSummaryResponse(
        UUID id,
        EstadoPedido status,
        int totalItems,
        BigDecimal total,
        Instant createdAt
) {}
