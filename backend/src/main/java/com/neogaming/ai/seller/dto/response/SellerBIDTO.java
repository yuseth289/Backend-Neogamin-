package com.neogaming.ai.seller.dto.response;

import java.util.List;

public record SellerBIDTO(
        String narrative,
        List<SellerBIKPIDTO> kpis,
        List<String> recommendations,
        int processingTimeMs
) {}
