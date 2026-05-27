package com.neogaming.ai.seller.dto.response;

import java.util.List;

public record ImageAnalysisDTO(
        int imageIndex,
        double qualityScore,
        List<String> issues,
        List<String> recommendations,
        String backgroundType,
        boolean needsBackgroundRemoval,
        String lightingQuality,
        String sharpness
) {}
