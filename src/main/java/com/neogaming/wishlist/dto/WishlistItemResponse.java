package com.neogaming.wishlist.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de salida con los datos de un producto en una lista de deseos.
 * Incluye información básica del producto para mostrar en la lista.
 */
public record WishlistItemResponse(
        UUID itemId,
        UUID productId,
        String productName,
        String productSlug,
        String productImageUrl,
        BigDecimal finalPrice,
        boolean inStock,
        Instant addedAt
) {}
