package com.neogaming.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para ajustar el stock físico de un producto.
 *
 * Usado en dos casos:
 * 1. El vendedor actualiza el stock disponible (IN: agrega unidades)
 * 2. El vendedor corrige el stock manualmente (ADJUST: establece valor absoluto)
 *
 * Ejemplo de request body:
 * {
 *   "quantity": 50,
 *   "notes":    "Recepción de mercancía — Orden de compra #OP-2026-001"
 * }
 */
public record StockAdjustRequest(

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 0, message = "La cantidad no puede ser negativa")
        Integer quantity,

        @Size(max = 300, message = "Las notas no pueden superar los 300 caracteres")
        String notes
) {}
