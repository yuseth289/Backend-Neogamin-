package com.neogaming.order.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de salida con los datos de un ítem de la orden.
 */
public record OrderItemResponse(
        UUID productId,
        String productName,
        String productSku,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
