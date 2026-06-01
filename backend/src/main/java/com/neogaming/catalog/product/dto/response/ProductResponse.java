package com.neogaming.catalog.product.dto.response;

import com.neogaming.common.enums.EstadoProducto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO de salida con los datos completos de un producto.
 *
 * El campo finalPrice se calcula en el mapper:
 *   finalPrice = basePrice × (1 + ivaPercent / 100)
 *
 * Ejemplo de respuesta JSON:
 * {
 *   "id":          "uuid",
 *   "name":        "Teclado Mecánico Logitech G Pro X",
 *   "basePrice":   350000.00,
 *   "ivaPercent":  19.00,
 *   "finalPrice":  416500.00,
 *   "status":      "ACTIVE",
 *   ...
 * }
 */
public record ProductResponse(

        UUID id,
        UUID sellerId,
        UUID categoryId,

        String name,
        String slug,
        String description,
        String brand,
        String sku,

        /** Precio sin IVA en COP */
        BigDecimal basePrice,

        /** Porcentaje de IVA (generalmente 19.00) */
        BigDecimal ivaPercent,

        /**
         * Precio final con IVA incluido en COP.
         * Calculado: basePrice × (1 + ivaPercent / 100)
         */
        BigDecimal finalPrice,

        EstadoProducto status,

        /** Unidades disponibles para compra (physicalStock - reservedStock) */
        Integer availableStock,

        /** Imágenes del producto ordenadas (principal primero) */
        List<ProductImageResponse> images,

        Instant createdAt,
        Instant updatedAt,

        /** Características del producto como pares clave-valor */
        Map<String, String> specifications,

        /** Porcentaje de descuento de la oferta vigente. null si no hay oferta activa. */
        java.math.BigDecimal activeDiscountPercent
) {}
