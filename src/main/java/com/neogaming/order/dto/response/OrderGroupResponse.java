package com.neogaming.order.dto.response;

import com.neogaming.common.enums.EstadoGrupo;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO de salida con los datos de un grupo de la orden (por vendedor).
 */
public record OrderGroupResponse(
        UUID id,
        UUID sellerId,
        EstadoGrupo status,
        BigDecimal subtotal,
        String trackingNumber,
        List<OrderItemResponse> items
) {}
