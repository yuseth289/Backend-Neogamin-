package com.neogaming.inventory.dto.response;

import com.neogaming.common.enums.TipoMovimientoStock;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de salida con los datos de un movimiento de inventario.
 * Los movimientos son inmutables — este DTO solo se usa para lectura.
 */
public record InventoryMovementResponse(

        UUID id,

        /** Tipo de movimiento (IN, OUT, RESERVE, RELEASE, ADJUST) */
        TipoMovimientoStock movementType,

        /** Cantidad de unidades del movimiento */
        int quantity,

        /** Stock físico resultante después del movimiento */
        int physicalAfter,

        /** Stock reservado resultante después del movimiento */
        int reservedAfter,

        /** UUID del documento relacionado (checkout u orden). null para ajustes manuales. */
        UUID referenceId,

        /** Descripción del motivo del movimiento */
        String notes,

        /** Fecha y hora exacta del movimiento */
        Instant createdAt
) {}
