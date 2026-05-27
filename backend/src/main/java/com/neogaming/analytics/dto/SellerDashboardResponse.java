package com.neogaming.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Respuesta del dashboard de analíticas para el vendedor.
 * Muestra métricas del mes actual y del mes anterior para comparación.
 */
public record SellerDashboardResponse(
        // ─── Mes actual ─────────────────────────────────────────────
        long ordenesEsteMes,
        BigDecimal ingresosEsteMes,
        long unidadesVendidasEsteMes,

        // ─── Mes anterior (para comparación) ─────────────────────────
        long ordenesMesAnterior,
        BigDecimal ingresosMesAnterior,

        // ─── Acumulado total ─────────────────────────────────────────
        long ordenesTotales,
        BigDecimal ingresosTotales,

        // ─── Estado de órdenes activas ───────────────────────────────
        long ordenesPendientes,
        long ordenesEnPreparacion,
        long ordenesEnviadas,

        // ─── Top 5 productos más vendidos ────────────────────────────
        List<TopProductoResponse> topProductos,

        // ─── Valoración promedio de reseñas ─────────────────────────
        double promedioCalificacion,
        long totalResenas
) {}
