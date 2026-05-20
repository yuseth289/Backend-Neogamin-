package com.neogaming.checkout.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de salida con los datos de un ítem del checkout.
 */
public record CheckoutItemResponse(
        UUID productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
