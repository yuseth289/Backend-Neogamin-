package com.neogaming.order.dto.response;

import com.neogaming.common.enums.EstadoGrupo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SellerOrderDetailResponse(
        UUID id,
        UUID orderId,
        String buyerName,
        String buyerEmail,
        Object shippingAddress,
        EstadoGrupo status,
        BigDecimal subtotal,
        String trackingNumber,
        List<OrderItemResponse> items,
        Instant createdAt
) {}
