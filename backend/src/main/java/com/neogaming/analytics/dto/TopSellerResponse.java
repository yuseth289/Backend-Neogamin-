package com.neogaming.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TopSellerResponse(
        UUID sellerId,
        String storeName,
        BigDecimal revenue,
        long ordenes,
        long unidades
) {}
