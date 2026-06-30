package com.neogaming.ai.search.dto.response;

import java.util.List;
import java.util.Map;

public record SearchResultDTO(
        String greeting,
        List<ProductRecommendationDTO> recommendations,
        Map<String, Object> structuredFilters,
        boolean needsClarification,
        String clarificationQuestion,
        String intentClassified,
        Integer processingTimeMs
) {

    public record ProductRecommendationDTO(
            String productId,
            String slug,
            String productName,
            Long price,
            String priceFormatted,
            double relevanceScore,
            String explanation,
            String compatibilityNotes,
            boolean priceFit,
            String imageUrl,
            boolean stockAvailable
    ) {}
}
