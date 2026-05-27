package com.neogaming.seller.controller;

import com.neogaming.common.response.ApiResponse;
import com.neogaming.common.response.PageResponse;
import com.neogaming.common.util.SecurityUtils;
import com.neogaming.order.dto.request.UpdateGroupStatusRequest;
import com.neogaming.order.dto.response.SellerOrderDetailResponse;
import com.neogaming.order.dto.response.SellerOrderSummaryResponse;
import com.neogaming.order.service.OrderService;
import com.neogaming.seller.dto.request.PaymentAccountRequest;
import com.neogaming.seller.dto.request.SellerRegistrationRequest;
import com.neogaming.seller.dto.request.UpdateSellerRequest;
import com.neogaming.seller.dto.response.PaymentAccountResponse;
import com.neogaming.seller.dto.response.PublicSellerResponse;
import com.neogaming.seller.dto.response.SellerResponse;
import com.neogaming.seller.service.SellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la gestión de vendedores en NeoGaming.
 *
 * Endpoints públicos (sin autenticación):
 *  GET  /sellers/{storeSlug}              → Ver tienda pública
 *
 * Endpoints del vendedor autenticado (requieren JWT):
 *  POST /sellers/register                 → Solicitar convertirse en vendedor
 *  GET  /sellers/me                       → Ver mi perfil de vendedor
 *  PUT  /sellers/me                       → Actualizar mi perfil
 *  GET  /sellers/me/accounts              → Listar mis cuentas bancarias
 *  POST /sellers/me/accounts              → Agregar cuenta bancaria
 *  PATCH /sellers/me/accounts/{id}/activate → Activar cuenta bancaria
 *  DELETE /sellers/me/accounts/{id}       → Eliminar cuenta bancaria
 */
@RestController
@RequestMapping("/sellers")
@RequiredArgsConstructor
@Tag(name = "Vendedores", description = "Gestión de perfiles de vendedor y cuentas bancarias")
public class SellerController {

    private final SellerService sellerService;
    private final OrderService orderService;

    // ===== ENDPOINTS PÚBLICOS =====

    /**
     * Retorna la información pública de una tienda para los compradores.
     * No requiere autenticación.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PublicSellerResponse>>> buscarTiendas(
            @RequestParam(defaultValue = "") String q,
            @PageableDefault(size = 8, sort = "storeName") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(sellerService.buscarTiendasPublicas(q, pageable)));
    }

    @PostMapping("/{id}/follow")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> seguir(@PathVariable UUID id) {
        sellerService.seguirTienda(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @DeleteMapping("/{id}/follow")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> dejarDeSeguir(@PathVariable UUID id) {
        sellerService.dejarDeSeguirTienda(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @GetMapping("/{id}/follow")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Boolean>> isFollowing(@PathVariable UUID id) {
        boolean following = sellerService.estaSigniendo(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(following));
    }

    @GetMapping("/followed")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<List<PublicSellerResponse>>> tiendasSeguidas() {
        List<PublicSellerResponse> list = sellerService.listarTiendasSeguidas(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/{storeSlug}")
    @Operation(
            summary = "Ver tienda pública",
            description = "Retorna los datos públicos de una tienda activa identificada por su slug."
    )
    public ResponseEntity<ApiResponse<PublicSellerResponse>> verTiendaPublica(
            @PathVariable String storeSlug) {
        PublicSellerResponse response = sellerService.obtenerTiendaPublica(storeSlug);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ===== ENDPOINTS DEL VENDEDOR =====

    /**
     * Solicita convertirse en vendedor. El perfil inicia en estado PENDING.
     * Solo para usuarios autenticados con cualquier rol.
     */
    @PostMapping("/register")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Registrarse como vendedor",
            description = "Crea un perfil de vendedor en estado PENDIENTE. Un administrador debe aprobarlo."
    )
    public ResponseEntity<ApiResponse<SellerResponse>> registrar(
            @Valid @RequestBody SellerRegistrationRequest request) {
        SellerResponse response = sellerService.registrarVendedor(
                request, SecurityUtils.getCurrentUserId()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    /**
     * Retorna el perfil del vendedor autenticado.
     */
    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(
            summary = "Ver mi perfil de vendedor",
            description = "Retorna el perfil completo del vendedor autenticado."
    )
    public ResponseEntity<ApiResponse<SellerResponse>> obtenerMiPerfil() {
        SellerResponse response = sellerService.obtenerMiPerfil(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Actualiza los datos editables del perfil del vendedor.
     */
    @PutMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(
            summary = "Actualizar mi perfil",
            description = "Actualiza nombre de tienda, descripción y datos de contacto."
    )
    public ResponseEntity<ApiResponse<SellerResponse>> actualizarPerfil(
            @Valid @RequestBody UpdateSellerRequest request) {
        SellerResponse response = sellerService.actualizarPerfil(
                request, SecurityUtils.getCurrentUserId()
        );
        return ResponseEntity.ok(ApiResponse.ok("Perfil actualizado correctamente", response));
    }

    // ===== ENDPOINTS DE CUENTAS BANCARIAS =====

    /**
     * Lista todas las cuentas bancarias registradas por el vendedor.
     */
    @GetMapping("/me/accounts")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(
            summary = "Listar mis cuentas bancarias",
            description = "Retorna todas las cuentas bancarias del vendedor con número enmascarado."
    )
    public ResponseEntity<ApiResponse<List<PaymentAccountResponse>>> listarCuentas() {
        List<PaymentAccountResponse> cuentas = sellerService.listarCuentas(
                SecurityUtils.getCurrentUserId()
        );
        return ResponseEntity.ok(ApiResponse.ok(cuentas));
    }

    /**
     * Registra una nueva cuenta bancaria para recibir pagos.
     */
    @PostMapping("/me/accounts")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(
            summary = "Agregar cuenta bancaria",
            description = "Registra una nueva cuenta bancaria. Queda inactiva hasta que se active explícitamente."
    )
    public ResponseEntity<ApiResponse<PaymentAccountResponse>> agregarCuenta(
            @Valid @RequestBody PaymentAccountRequest request) {
        PaymentAccountResponse response = sellerService.agregarCuenta(
                request, SecurityUtils.getCurrentUserId()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    /**
     * Activa una cuenta bancaria y desactiva las demás automáticamente.
     */
    @PatchMapping("/me/accounts/{id}/activate")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(
            summary = "Activar cuenta bancaria",
            description = "Establece esta cuenta como la activa para recibir pagos. Las demás se desactivan."
    )
    public ResponseEntity<ApiResponse<PaymentAccountResponse>> activarCuenta(
            @PathVariable UUID id) {
        PaymentAccountResponse response = sellerService.activarCuenta(
                id, SecurityUtils.getCurrentUserId()
        );
        return ResponseEntity.ok(ApiResponse.ok("Cuenta bancaria activada correctamente", response));
    }

    /**
     * Elimina una cuenta bancaria (solo si no está activa).
     */
    @DeleteMapping("/me/accounts/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(
            summary = "Eliminar cuenta bancaria",
            description = "Elimina una cuenta bancaria. No se puede eliminar la cuenta activa."
    )
    public ResponseEntity<ApiResponse<Void>> eliminarCuenta(@PathVariable UUID id) {
        sellerService.eliminarCuenta(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    // ===== ENDPOINTS DE ÓRDENES DEL VENDEDOR =====

    @GetMapping("/me/orders")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Listar órdenes recibidas", description = "Retorna los grupos de orden asignados al vendedor autenticado.")
    public ResponseEntity<ApiResponse<PageResponse<SellerOrderSummaryResponse>>> listarOrdenes(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        UUID sellerId = sellerService.obtenerSellerIdByUserId(SecurityUtils.getCurrentUserId());
        PageResponse<SellerOrderSummaryResponse> page = orderService.listarGruposVendedor(sellerId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/me/orders/{groupId}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Ver detalle de orden", description = "Retorna el detalle completo de un grupo de orden del vendedor.")
    public ResponseEntity<ApiResponse<SellerOrderDetailResponse>> verOrden(@PathVariable UUID groupId) {
        UUID sellerId = sellerService.obtenerSellerIdByUserId(SecurityUtils.getCurrentUserId());
        SellerOrderDetailResponse detail = orderService.obtenerGrupoVendedor(groupId, sellerId);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    @PatchMapping("/me/orders/{groupId}/status")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Actualizar estado de orden", description = "Actualiza el estado del grupo de orden. Permite agregar número de seguimiento al marcar como SHIPPED.")
    public ResponseEntity<ApiResponse<SellerOrderDetailResponse>> actualizarEstado(
            @PathVariable UUID groupId,
            @Valid @RequestBody UpdateGroupStatusRequest request) {
        UUID sellerId = sellerService.obtenerSellerIdByUserId(SecurityUtils.getCurrentUserId());
        SellerOrderDetailResponse detail = orderService.actualizarEstadoGrupoSeller(
                groupId, request.status(), request.trackingNumber(), sellerId
        );
        return ResponseEntity.ok(ApiResponse.ok("Estado actualizado correctamente", detail));
    }
}
