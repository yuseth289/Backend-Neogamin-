package com.neogaming.chat.dto.response;

import com.neogaming.common.enums.RolMensaje;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        RolMensaje senderRole,
        String senderName,
        String senderAvatar,
        String content,
        boolean readByBuyer,
        boolean readBySeller,
        Instant createdAt
) {}
