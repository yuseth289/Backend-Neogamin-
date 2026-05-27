package com.neogaming.seller.dto.response;

import java.util.UUID;

/**
 * DTO de salida con la información pública de una tienda.
 *
 * Solo expone los datos visibles para compradores no autenticados.
 * Oculta información fiscal, datos de contacto privados y tokens de pago.
 */
public record PublicSellerResponse(

        UUID id,

        String storeName,

        String storeSlug,

        String storeDescription,

        String storeLogoUrl,

        String storeBannerUrl,

        String city,

        String department,

        Long totalSales,

        Double averageRating,

        Long totalReviews
) {}
