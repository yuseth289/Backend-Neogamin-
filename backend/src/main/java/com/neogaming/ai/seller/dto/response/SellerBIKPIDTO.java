package com.neogaming.ai.seller.dto.response;

public record SellerBIKPIDTO(
        String name,
        Object value,
        String unit,
        String period,
        Double variationPct,
        String trend,
        boolean isAlert
) {}
