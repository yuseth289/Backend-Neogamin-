package com.neogaming.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Respuesta del dashboard de analíticas para el administrador.
 * Visión global de la plataforma.
 */
public record AdminDashboardResponse(
        // ─── Usuarios ────────────────────────────────────────────────
        long totalUsuarios,
        long usuariosNuevosEsteMes,
        long totalVendedores,
        long vendedoresPendientesAprobacion,

        // ─── Órdenes ─────────────────────────────────────────────────
        long ordenesTotales,
        long ordenesEsteMes,
        long ordenesPendientes,

        // ─── Facturación ─────────────────────────────────────────────
        BigDecimal ingresosTotales,
        BigDecimal ingresosEsteMes,
        BigDecimal comisionesEsteMes,

        // ─── Pagos ───────────────────────────────────────────────────
        long pagosAprobadosEsteMes,
        long pagosRechazadosEsteMes,
        BigDecimal montoAprobadoEsteMes,

        // ─── Catálogo ────────────────────────────────────────────────
        long totalProductosActivos,
        long productosPendientesRevision,

        // ─── Top 10 productos globales ───────────────────────────────
        List<TopProductoResponse> topProductos
) {}
