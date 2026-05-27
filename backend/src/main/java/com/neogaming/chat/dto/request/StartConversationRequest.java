package com.neogaming.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StartConversationRequest(
        @NotNull UUID sellerId,
        UUID productId,
        @NotBlank @Size(max = 2000) String firstMessage
) {}
