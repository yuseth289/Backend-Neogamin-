package com.neogaming.ai.seller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerBIRequest(
        @NotBlank @Size(max = 500) String query
) {}
