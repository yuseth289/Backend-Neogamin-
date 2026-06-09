package com.neogaming.ai.analytics.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record AnalyticsAIRequest(
        @NotBlank String query,
        String adminId,
        LocalDate dateFrom,
        LocalDate dateTo,
        String reportType
) {
    public AnalyticsAIRequest(String query, String adminId) {
        this(query, adminId, null, null, "adhoc");
    }
}
