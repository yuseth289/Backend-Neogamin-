package com.neogaming.wishlist.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO de salida con los datos de una lista de deseos y sus ítems.
 */
public record WishlistResponse(
        UUID id,
        String name,
        boolean isPublic,
        List<WishlistItemResponse> items,
        Instant createdAt
) {}
