package com.neogaming.catalog.offer.controller;

import com.neogaming.catalog.offer.dto.request.OfferRequest;
import com.neogaming.catalog.offer.dto.response.OfferResponse;
import com.neogaming.catalog.offer.service.OfferService;
import com.neogaming.common.response.ApiResponse;
import com.neogaming.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la gestión de ofertas de descuento.
 *
 * Endpoints del vendedor (requieren rol SELLER):
 *  GET    /products/me/{productId}/offers              → Listar mis ofertas
 *  POST   /products/me/{productId}/offers              → Crear oferta
 *  DELETE /products/me/{productId}/offers/{offerId}    → Desactivar oferta
 *
 * Endpoint público:
 *  GET    /products/{productId}/offers/current         → Oferta vigente actual
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Ofertas", description = "Gestión de descuentos y promociones sobre productos")
public class OfferController {

    private final OfferService offerService;

    // ===== ENDPOINT PÚBLICO =====

    /**
     * Retorna la oferta vigente actual de un producto.
     * Usado en el detalle del producto para mostrar el precio con descuento.
     * null si no hay oferta activa en este momento.
     */
    @GetMapping("/products/{productId}/offers/current")
    @Operation(
            summary = "Oferta vigente del producto",
            description = "Retorna la oferta activa actual del producto, o null si no hay ninguna."
    )
    public ResponseEntity<ApiResponse<OfferResponse>> obtenerVigente(@PathVariable UUID productId) {
        OfferResponse response = offerService.obtenerVigente(productId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ===== ENDPOINTS DEL VENDEDOR =====

    /**
     * Lista todas las ofertas de un producto del vendedor autenticado.
     */
    @GetMapping("/products/me/{productId}/offers")
    @PreAuthorize("hasRole('SELLER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Listar ofertas del producto",
            description = "Retorna todas las ofertas (activas e inactivas) de un producto del vendedor."
    )
    public ResponseEntity<ApiResponse<List<OfferResponse>>> listar(@PathVariable UUID productId) {
        List<OfferResponse> ofertas = offerService.listar(productId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(ofertas));
    }

    /**
     * Crea una nueva oferta de descuento para un producto del vendedor.
     */
    @PostMapping("/products/me/{productId}/offers")
    @PreAuthorize("hasRole('SELLER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Crear oferta",
            description = "Crea una oferta de descuento (PERCENTAGE o FIXED) con período de vigencia."
    )
    public ResponseEntity<ApiResponse<OfferResponse>> crear(
            @PathVariable UUID productId,
            @Valid @RequestBody OfferRequest request) {
        OfferResponse response = offerService.crear(productId, request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    /**
     * Desactiva una oferta de un producto del vendedor.
     */
    @DeleteMapping("/products/me/{productId}/offers/{offerId}")
    @PreAuthorize("hasRole('SELLER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Desactivar oferta",
            description = "Desactiva una oferta activa. El producto vuelve a su precio normal."
    )
    public ResponseEntity<ApiResponse<Void>> desactivar(
            @PathVariable UUID productId,
            @PathVariable UUID offerId) {
        offerService.desactivar(productId, offerId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
