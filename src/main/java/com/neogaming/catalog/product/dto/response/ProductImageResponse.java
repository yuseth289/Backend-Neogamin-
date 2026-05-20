package com.neogaming.catalog.product.dto.response;

import java.util.UUID;

/**
 * DTO de salida con los datos de una imagen de producto.
 */
public record ProductImageResponse(
        UUID id,
        String url,
        String altText,
        int sortOrder,
        /** true si es la imagen principal del producto */
        boolean primary
) {}
