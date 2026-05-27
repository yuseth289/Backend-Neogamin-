package com.neogaming.order.dto.response;

import com.neogaming.common.enums.EstadoGrupo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SellerOrderSummaryResponse(
        UUID id,
        UUID orderId,
        String buyerName,
        EstadoGrupo status,
        int totalItems,
        BigDecimal subtotal,
        String trackingNumber,
        Instant createdAt
) {}
