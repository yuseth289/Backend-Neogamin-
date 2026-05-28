package com.neogaming.catalog.brand.dto;

import java.util.UUID;

public record BrandResponse(
        UUID id,
        String name,
        String slug,
        int displayOrder,
        boolean active
) {}
