package com.neogaming.ai.analytics.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record AnalyticsAIRequest(
        @NotBlank String query,
        LocalDate dateFrom,
        LocalDate dateTo,
        String reportType
) {
    public AnalyticsAIRequest(String query) {
        this(query, null, null, "adhoc");
    }
}
