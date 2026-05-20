package com.neogaming.cart.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO de salida con el carrito completo del usuario.
 *
 * Incluye todos los ítems, el total y una advertencia si algún precio
 * cambió desde que el ítem fue agregado.
 */
public record CartResponse(

        UUID id,

        /** Ítems del carrito con detalles del producto */
        List<CartItemResponse> items,

        /** Total del carrito en COP (suma de subtotales de todos los ítems) */
        BigDecimal total,

        /** Número total de unidades en el carrito (suma de quantities) */
        int totalItems,

        /**
         * true si algún ítem tiene un precio distinto al guardado.
         * Permite al frontend mostrar una advertencia global sin revisar cada ítem.
         */
        boolean hasPriceChanges
) {}
