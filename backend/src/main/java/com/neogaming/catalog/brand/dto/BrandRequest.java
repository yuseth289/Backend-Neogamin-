package com.neogaming.catalog.brand.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrandRequest(

        @NotBlank(message = "El nombre de la marca es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,

        int displayOrder
) {}
