package com.neogaming.ai.seller.dto.response;

import java.util.List;

public record ImageEnhancementDTO(
        List<EnhancedImageResultDTO> enhancedImages,
        String promotionalImageBase64,
        int totalProcessingTimeMs,
        String providerUsed,
        double overallQualityImprovement
) {
    public record EnhancedImageResultDTO(
            int originalIndex,
            String enhancedBase64,
            double qualityBefore,
            double qualityAfter,
            List<String> operationsApplied,
            String modificationSummary
    ) {}
}
