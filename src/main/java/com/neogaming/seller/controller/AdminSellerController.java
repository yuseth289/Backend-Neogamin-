package com.neogaming.seller.controller;

import com.neogaming.common.enums.EstadoGenerico;
import com.neogaming.common.response.ApiResponse;
import com.neogaming.common.response.PageResponse;
import com.neogaming.seller.dto.response.SellerResponse;
import com.neogaming.seller.service.SellerService;
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
 * Controlador REST para la gestión administrativa de vendedores.
 *
 * Todos los endpoints requieren rol ADMIN.
 *
 * Endpoints disponibles:
 *  GET   /admin/sellers               → Listar vendedores por estado
 *  PATCH /admin/sellers/{id}/approve  → Aprobar solicitud de vendedor
 *  PATCH /admin/sellers/{id}/suspend  → Suspender vendedor
 */
@RestController
@RequestMapping("/admin/sellers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin — Vendedores", description = "Gestión administrativa de vendedores (solo ADMIN)")
public class AdminSellerController {

    private final SellerService sellerService;

    /**
     * Lista todos los vendedores filtrados por estado.
     * Útil para revisar solicitudes pendientes de aprobación.
     *
     * @param status   Estado a filtrar (PENDING, ACTIVE, SUSPENDED). Por defecto PENDING.
     * @param pageable Configuración de paginación
     */
    @GetMapping
    @Operation(
            summary = "Listar vendedores por estado",
            description = "Retorna vendedores paginados filtrados por estado. Por defecto muestra los PENDIENTES."
    )
    public ResponseEntity<ApiResponse<PageResponse<SellerResponse>>> listar(
            @RequestParam(defaultValue = "PENDING") EstadoGenerico status,
            @PageableDefault(size = 20) Pageable pageable) {

        PageResponse<SellerResponse> page = sellerService.listarPorEstado(status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    /**
     * Aprueba la solicitud de un vendedor.
     * Cambia su estado de PENDING a ACTIVE, habilitándolo para publicar productos.
     *
     * @param id UUID del perfil de vendedor a aprobar
     */
    @PatchMapping("/{id}/approve")
    @Operation(
            summary = "Aprobar vendedor",
            description = "Aprueba la solicitud de un vendedor en estado PENDIENTE."
    )
    public ResponseEntity<ApiResponse<SellerResponse>> aprobar(@PathVariable UUID id) {
        SellerResponse response = sellerService.aprobar(id);
        return ResponseEntity.ok(ApiResponse.ok("Vendedor aprobado correctamente", response));
    }

    /**
     * Suspende un vendedor activo.
     * Sus productos dejan de aparecer en el catálogo mientras esté suspendido.
     *
     * @param id UUID del perfil de vendedor a suspender
     */
    @PatchMapping("/{id}/suspend")
    @Operation(
            summary = "Suspender vendedor",
            description = "Suspende un vendedor activo. Sus productos quedan ocultos en el catálogo."
    )
    public ResponseEntity<ApiResponse<SellerResponse>> suspender(@PathVariable UUID id) {
        SellerResponse response = sellerService.suspender(id);
        return ResponseEntity.ok(ApiResponse.ok("Vendedor suspendido", response));
    }
}
