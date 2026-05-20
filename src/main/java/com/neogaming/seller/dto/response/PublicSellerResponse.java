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

        /** Nombre público de la tienda */
        String storeName,

        /** Slug para construir la URL de la tienda (ej: /sellers/gaming-shop) */
        String storeSlug,

        /** Descripción visible para los compradores */
        String storeDescription,

        /** URL del logo de la tienda */
        String storeLogoUrl,

        /** URL del banner/portada de la tienda */
        String storeBannerUrl,

        /** Ciudad donde opera la tienda */
        String city,

        /** Departamento donde opera la tienda */
        String department
) {}
