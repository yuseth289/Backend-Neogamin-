package com.neogaming.order.controller;

import com.neogaming.common.enums.EstadoGrupo;
import com.neogaming.common.response.ApiResponse;
import com.neogaming.common.response.PageResponse;
import com.neogaming.common.util.SecurityUtils;
import com.neogaming.order.dto.response.OrderResponse;
import com.neogaming.order.dto.response.OrderSummaryResponse;
import com.neogaming.order.service.OrderService;
import com.neogaming.seller.repository.SellerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para la gestión de órdenes en NeoGaming.
 *
 * Endpoints del comprador (requieren JWT):
 *  GET  /orders              → Mis órdenes
 *  GET  /orders/{id}         → Detalle de una orden
 *
 * Endpoints del vendedor (requieren rol SELLER):
 *  PATCH /orders/{id}/groups/{groupId}/status → Actualizar estado del grupo
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Órdenes", description = "Gestión de órdenes de compra")
public class OrderController {

    private final OrderService orderService;
    private final SellerRepository sellerRepository;

    // ===== ENDPOINTS DEL COMPRADOR =====

    /**
     * Lista todas las órdenes del comprador autenticado.
     */
    @GetMapping
    @Operation(
            summary = "Mis órdenes",
            description = "Retorna todas las órdenes del comprador autenticado, de más reciente a más antigua."
    )
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> listarMisOrdenes(
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<OrderSummaryResponse> page = orderService.listarMisOrdenes(
                SecurityUtils.getCurrentUserId(), pageable
        );
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    /**
     * Retorna el detalle completo de una orden del comprador.
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Detalle de orden",
            description = "Retorna la orden con todos sus grupos e ítems, incluida la dirección de entrega."
    )
    public ResponseEntity<ApiResponse<OrderResponse>> obtenerMiOrden(@PathVariable UUID id) {
        OrderResponse response = orderService.obtenerMiOrden(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ===== ENDPOINTS DEL VENDEDOR =====

    /**
     * Permite al vendedor actualizar el estado de su grupo en la orden.
     * Por ejemplo, marcar como SHIPPED con número de seguimiento.
     */
    @PatchMapping("/{orderId}/groups/{groupId}/status")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(
            summary = "Actualizar estado del grupo",
            description = "El vendedor actualiza el estado de preparación/envío de su grupo de ítems."
    )
    public ResponseEntity<ApiResponse<OrderResponse>> actualizarEstadoGrupo(
            @PathVariable UUID orderId,
            @PathVariable UUID groupId,
            @RequestParam EstadoGrupo status,
            @RequestParam(required = false) String trackingNumber) {

        // Obtener el sellerId del perfil del vendedor autenticado
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID sellerId = sellerRepository.findByUserId(userId)
                .map(s -> s.getId())
                .orElseThrow(() -> new com.neogaming.common.exception.BusinessRuleException(
                        "No tienes un perfil de vendedor activo",
                        "VENDEDOR_NO_ENCONTRADO"
                ));

        OrderResponse response = orderService.actualizarEstadoGrupo(
                orderId, groupId, status, trackingNumber, sellerId
        );
        return ResponseEntity.ok(ApiResponse.ok("Estado del grupo actualizado", response));
    }
}
