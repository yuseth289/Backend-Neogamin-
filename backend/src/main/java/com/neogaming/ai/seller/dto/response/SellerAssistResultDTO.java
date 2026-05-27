package com.neogaming.ai.seller.dto.response;

import java.util.List;

public record SellerAssistResultDTO(
        OptimizedContentDTO optimizedContent,
        ListingQualityScoreDTO listingScore,
        List<ImageAnalysisDTO> imageAnalysis,
        int processingTimeMs
) {
    public record OptimizedContentDTO(
            String seoTitle,
            String commercialDescription,
            List<String> keyBenefits,
            List<String> seoKeywords,
            List<String> tags
    ) {}

    public record ListingQualityScoreDTO(
            double totalScore,
            double contentScore,
            double completenessScore,
            double seoScore,
            double imageScore,
            List<String> missingFields,
            List<String> improvementSuggestions
    ) {}
}
