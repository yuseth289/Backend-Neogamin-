package com.neogaming.internal.analytics.controller;

import com.neogaming.analytics.dto.AdminDashboardResponse;
import com.neogaming.analytics.dto.SellerDashboardResponse;
import com.neogaming.analytics.dto.TopProductoResponse;
import com.neogaming.analytics.dto.TopSellerResponse;
import com.neogaming.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoints internos de analítica para el microservicio Python AI.
 * Solo accesibles con X-Internal-Token (validado por InternalTokenFilter).
 */
@RestController
@RequestMapping("/internal/analytics")
@RequiredArgsConstructor
public class InternalAnalyticsController {

    private final AnalyticsService analyticsService;

    // ── Endpoints existentes ──────────────────────────────────────────────────

    @GetMapping("/sales")
    public ResponseEntity<AdminDashboardResponse> getSalesData() {
        return ResponseEntity.ok(analyticsService.dashboardAdmin());
    }

    @GetMapping("/inventory")
    public ResponseEntity<Map<String, Object>> getInventoryData() {
        AdminDashboardResponse dashboard = analyticsService.dashboardAdmin();
        Map<String, Object> inventoryData = Map.of(
                "totalActiveProducts", dashboard.totalProductosActivos(),
                "totalPendingProducts", dashboard.productosPendientesRevision()
        );
        return ResponseEntity.ok(inventoryData);
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUsersData() {
        AdminDashboardResponse dashboard = analyticsService.dashboardAdmin();
        Map<String, Object> usersData = Map.of(
                "totalUsers", dashboard.totalUsuarios(),
                "newUsersThisMonth", dashboard.usuariosNuevosEsteMes(),
                "totalSellers", dashboard.totalVendedores(),
                "pendingSellers", dashboard.vendedoresPendientesAprobacion()
        );
        return ResponseEntity.ok(usersData);
    }

    // ── Seller BI — Dashboard de un vendedor específico ──────────────────────

    @GetMapping("/seller/{sellerId}/dashboard")
    public ResponseEntity<SellerDashboardResponse> getSellerDashboard(@PathVariable UUID sellerId) {
        return ResponseEntity.ok(analyticsService.dashboardVendedor(sellerId));
    }

    // ── Marketplace BI — Top sellers, trending, categories ───────────────────

    @GetMapping("/marketplace/top-sellers")
    public ResponseEntity<List<TopSellerResponse>> getTopSellers(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.topVendedoresPorIngreso(Math.min(limit, 50)));
    }

    @GetMapping("/marketplace/trending")
    public ResponseEntity<List<TopProductoResponse>> getTrendingProducts(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.topProductosTendencia(Math.min(days, 365), Math.min(limit, 50)));
    }

    @GetMapping("/marketplace/categories")
    public ResponseEntity<List<Map<String, Object>>> getCategoryBreakdown() {
        return ResponseEntity.ok(analyticsService.ingresosPorCategoria());
    }
}
