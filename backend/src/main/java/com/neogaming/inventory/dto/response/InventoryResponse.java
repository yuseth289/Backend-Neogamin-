package com.neogaming.inventory.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de salida con el estado actual del inventario de un producto.
 *
 * availableStock = physicalStock - reservedStock
 * (calculado en el mapper, no persistido en BD)
 *
 * Ejemplo de respuesta JSON:
 * {
 *   "productId":      "uuid",
 *   "physicalStock":  100,
 *   "reservedStock":  15,
 *   "availableStock": 85,
 *   "updatedAt":      "2026-05-19T10:30:00Z"
 * }
 */
public record InventoryResponse(

        UUID id,
        UUID productId,

        /** Unidades físicas en bodega del vendedor */
        int physicalStock,

        /** Unidades bloqueadas por checkouts pendientes */
        int reservedStock,

        /** Unidades disponibles para nuevas compras (physicalStock - reservedStock) */
        int availableStock,

        Instant updatedAt
) {}
