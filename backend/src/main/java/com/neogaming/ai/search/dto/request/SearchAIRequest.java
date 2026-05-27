package com.neogaming.ai.search.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SearchAIRequest(
        @NotBlank String query,
        String clarification
) {}
