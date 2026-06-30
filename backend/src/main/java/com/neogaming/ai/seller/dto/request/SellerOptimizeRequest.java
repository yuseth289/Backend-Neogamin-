package com.neogaming.ai.seller.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record SellerOptimizeRequest(
        @NotBlank String name,
        String category,
        String brand,
        String model,
        Long priceCop,
        String rawDescription,
        List<String> features,
        List<String> imagesBase64,
        String sellerId,
        String instruction
) {}
