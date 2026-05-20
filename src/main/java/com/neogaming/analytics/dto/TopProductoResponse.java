package com.neogaming.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Datos de un producto en el ranking de los más vendidos.
 */
public record TopProductoResponse(
        UUID productId,
        String productName,
        long unidadesVendidas,
        BigDecimal ingresosTotales,
        long ordenes
) {}
