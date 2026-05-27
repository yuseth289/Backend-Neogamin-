package com.neogaming.chat.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID buyerId,
        String buyerName,
        UUID sellerId,
        String storeName,
        String storeSlug,
        String storeLogoUrl,
        UUID productId,
        String productName,
        String lastMessage,
        Instant lastMessageAt,
        int unreadCount,
        Instant createdAt
) {}
