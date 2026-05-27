package com.neogaming.analytics.controller;

import com.neogaming.analytics.dto.AdminDashboardResponse;
import com.neogaming.analytics.dto.SellerDashboardResponse;
import com.neogaming.analytics.service.AnalyticsService;
import com.neogaming.common.exception.BusinessRuleException;
import com.neogaming.common.response.ApiResponse;
import com.neogaming.common.util.SecurityUtils;
import com.neogaming.seller.repository.SellerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controlador REST para los dashboards de analíticas en NeoGaming.
 *
 * Endpoints del vendedor (requieren rol SELLER):
 *  GET /analytics/seller → Dashboard con métricas de la tienda
 *
 * Endpoints de administración (requieren rol ADMIN):
 *  GET /analytics/admin  → Dashboard global de la plataforma
 */
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Analíticas", description = "Dashboards de métricas para vendedores y administradores")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final SellerRepository sellerRepository;

    /**
     * Retorna el dashboard de analíticas del vendedor autenticado.
     * Incluye ventas del mes, ingresos, top productos y estado de órdenes activas.
     */
    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(
            summary = "Dashboard del vendedor",
            description = "Retorna métricas del mes actual, mes anterior y acumulados históricos " +
                          "para el vendedor autenticado."
    )
    public ResponseEntity<ApiResponse<SellerDashboardResponse>> dashboardVendedor() {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID sellerId = sellerRepository.findByUserId(userId)
                .map(s -> s.getId())
                .orElseThrow(() -> new BusinessRuleException(
                        "No tienes un perfil de vendedor activo",
                        "VENDEDOR_NO_ENCONTRADO"
                ));

        return ResponseEntity.ok(ApiResponse.ok(analyticsService.dashboardVendedor(sellerId)));
    }

    /**
     * Retorna el dashboard global de la plataforma para el administrador.
     * Incluye totales de usuarios, órdenes, pagos, catálogo y top 10 productos.
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Dashboard del administrador",
            description = "Retorna las métricas globales de la plataforma: usuarios, órdenes, " +
                          "facturación, pagos y catálogo."
    )
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> dashboardAdmin() {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.dashboardAdmin()));
    }
}
