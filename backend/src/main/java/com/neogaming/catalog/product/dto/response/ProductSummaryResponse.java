package com.neogaming.catalog.product.dto.response;

import com.neogaming.common.enums.EstadoProducto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de salida resumido para listados del catálogo.
 *
 * Se usa en paginaciones donde no se necesita la descripción completa
 * ni todas las imágenes — solo los datos para mostrar una tarjeta de producto.
 */
public record ProductSummaryResponse(

        UUID id,
        UUID sellerId,
        UUID categoryId,

        String name,
        String slug,
        String brand,

        /** Precio base sin IVA en COP */
        BigDecimal basePrice,

        /** Precio final con IVA incluido en COP */
        BigDecimal finalPrice,

        EstadoProducto status,

        /** Unidades disponibles (null si aún no se ha inicializado inventario) */
        Integer availableStock,

        /** URL de la imagen principal. null si no tiene imágenes. */
        String primaryImageUrl,

        /** Porcentaje de descuento de la oferta vigente. null si no hay oferta activa. */
        java.math.BigDecimal activeDiscountPercent
) {}
