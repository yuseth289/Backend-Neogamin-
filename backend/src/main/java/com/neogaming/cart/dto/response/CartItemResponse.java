package com.neogaming.cart.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de salida con los datos de un ítem del carrito.
 *
 * Incluye la información del producto para que el frontend
 * pueda mostrar la tarjeta sin consultar otro endpoint.
 */
public record CartItemResponse(

        UUID id,
        UUID productId,

        /** Nombre del producto (desnormalizado para el frontend) */
        String productName,

        /** URL de la imagen principal del producto */
        String productImageUrl,

        int quantity,

        /** Precio unitario al momento de agregar al carrito en COP */
        BigDecimal unitPrice,

        /** Subtotal del ítem: unitPrice × quantity en COP */
        BigDecimal subtotal,

        /**
         * true si el precio actual del producto difiere del precio guardado.
         * El frontend puede mostrar una advertencia de "el precio cambió".
         */
        boolean priceChanged,

        /** Precio actual del producto (para mostrar el nuevo precio si cambió) */
        BigDecimal currentPrice,

        /** Stock disponible para compra en este momento (physicalStock - reservedStock) */
        int availableStock,

        /** Slug del producto para construir la URL de detalle: /product/{slug} */
        String productSlug
) {}
