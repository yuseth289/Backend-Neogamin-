package com.neogaming.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO de entrada para agregar o actualizar un ítem en el carrito.
 *
 * Ejemplo de request body:
 * {
 *   "productId": "uuid-del-producto",
 *   "quantity":  2
 * }
 */
public record AddCartItemRequest(

        @NotNull(message = "El ID del producto es obligatorio")
        UUID productId,

        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        int quantity
) {}
