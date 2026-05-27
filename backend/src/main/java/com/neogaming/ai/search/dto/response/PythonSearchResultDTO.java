package com.neogaming.ai.search.dto.response;

import java.util.List;
import java.util.Map;

/** Maps exactly the JSON that the Python AI service returns — no enriched fields. */
public record PythonSearchResultDTO(
        List<RecommendationDTO> recommendations,
        Map<String, Object> structuredFilters,
        boolean needsClarification,
        String clarificationQuestion,
        String intentClassified,
        Integer processingTimeMs
) {
    public record RecommendationDTO(
            String productId,
            double relevanceScore,
            String explanation,
            String compatibilityNotes,
            boolean priceFit
    ) {}
}
