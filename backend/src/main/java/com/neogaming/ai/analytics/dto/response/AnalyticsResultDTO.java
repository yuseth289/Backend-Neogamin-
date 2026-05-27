package com.neogaming.ai.analytics.dto.response;

import java.util.List;
import java.util.Map;

public record AnalyticsResultDTO(
        String narrative,
        ExecutiveSummaryDTO summary,
        String queryIntent,
        int processingTimeMs
) {
    public record ExecutiveSummaryDTO(
            String title,
            String period,
            List<String> highlights,
            List<KPIResultDTO> kpis,
            List<Map<String, Object>> topProducts,
            List<String> alerts,
            List<String> recommendations,
            Map<String, Object> chartData
    ) {}

    public record KPIResultDTO(
            String name,
            Object value,
            String unit,
            String period,
            Double variationPct,
            String trend,
            boolean isAlert
    ) {}
}
