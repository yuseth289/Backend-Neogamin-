package com.neogaming.review.controller;

import com.neogaming.common.enums.EstadoResena;
import com.neogaming.common.response.ApiResponse;
import com.neogaming.common.response.PageResponse;
import com.neogaming.common.util.SecurityUtils;
import com.neogaming.review.dto.CreateReviewRequest;
import com.neogaming.review.dto.ProductRatingSummary;
import com.neogaming.review.dto.ReviewModerationRequest;
import com.neogaming.review.dto.ReviewResponse;
import com.neogaming.review.service.ReviewService;
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

import java.util.UUID;

/**
 * Controlador REST para la gestión de reseñas de productos en NeoGaming.
 *
 * Endpoints públicos (sin autenticación):
 *  GET  /products/{productId}/reviews         → Reseñas aprobadas de un producto
 *  GET  /products/{productId}/reviews/summary → Resumen de calificaciones
 *
 * Endpoints del comprador (requieren JWT):
 *  POST   /reviews                → Crear reseña
 *  GET    /reviews/me             → Mis reseñas
 *  DELETE /reviews/{id}           → Eliminar mi reseña (solo PENDING/REJECTED)
 *
 * Endpoints de administración (requieren rol ADMIN):
 *  GET    /admin/reviews          → Reseñas por estado para moderación
 *  PATCH  /admin/reviews/{id}/moderate → Aprobar o rechazar una reseña
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Reseñas", description = "Gestión y moderación de reseñas de productos")
public class ReviewController {

    private final ReviewService reviewService;

    // ===== ENDPOINTS PÚBLICOS =====

    /**
     * Lista las reseñas aprobadas de un producto de más reciente a más antigua.
     */
    @GetMapping("/products/{productId}/reviews")
    @Operation(
            summary = "Reseñas de un producto",
            description = "Retorna las reseñas aprobadas de un producto de forma paginada. Sin autenticación."
    )
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> listarResenasPorProducto(
            @PathVariable UUID productId,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<ReviewResponse> page = PageResponse.from(
                reviewService.listarResenasPorProducto(productId, pageable));
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    /**
     * Retorna el promedio de calificaciones y el total de reseñas aprobadas del producto.
     */
    @GetMapping("/products/{productId}/reviews/summary")
    @Operation(
            summary = "Resumen de calificaciones",
            description = "Retorna el promedio de estrellas y el número total de reseñas aprobadas. Sin autenticación."
    )
    public ResponseEntity<ApiResponse<ProductRatingSummary>> obtenerResumenRating(
            @PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.obtenerResumenRating(productId)));
    }

    // ===== ENDPOINTS DEL COMPRADOR =====

    /**
     * Crea una reseña para un producto comprado.
     * El usuario debe haber comprado el producto para poder reseñarlo.
     */
    @PostMapping("/reviews")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Crear reseña",
            description = "Crea una reseña para un producto comprado. " +
                          "La reseña queda en estado PENDING hasta que un administrador la modere."
    )
    public ResponseEntity<ApiResponse<ReviewResponse>> crearResena(
            @Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse response = reviewService.crearResena(request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    /**
     * Lista todas las reseñas del comprador autenticado en cualquier estado.
     */
    @GetMapping("/reviews/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Mis reseñas",
            description = "Retorna todas las reseñas del comprador autenticado (PENDING, APPROVED y REJECTED)."
    )
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> listarMisResenas(
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<ReviewResponse> page = PageResponse.from(
                reviewService.listarMisResenas(SecurityUtils.getCurrentUserId(), pageable));
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    /**
     * Elimina la propia reseña del comprador si está en estado PENDING o REJECTED.
     */
    @DeleteMapping("/reviews/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Eliminar mi reseña",
            description = "Elimina la reseña del comprador autenticado. " +
                          "Solo se pueden eliminar reseñas en estado PENDING o REJECTED."
    )
    public ResponseEntity<ApiResponse<Void>> eliminarMiResena(@PathVariable UUID id) {
        reviewService.eliminarMiResena(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    // ===== ENDPOINTS DE ADMINISTRACIÓN =====

    /**
     * Lista reseñas filtradas por estado para moderación.
     */
    @GetMapping("/admin/reviews")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Reseñas para moderar (Admin)",
            description = "Lista reseñas filtradas por estado. Por defecto retorna las PENDING."
    )
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> listarPorEstado(
            @RequestParam(defaultValue = "PENDING") EstadoResena status,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<ReviewResponse> page = PageResponse.from(
                reviewService.listarPorEstado(status, pageable));
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    /**
     * Aprueba o rechaza una reseña. Al rechazar, el motivo es obligatorio.
     */
    @PatchMapping("/admin/reviews/{id}/moderate")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Moderar reseña (Admin)",
            description = "Aprueba o rechaza una reseña. Si se rechaza, el campo rejectReason es obligatorio."
    )
    public ResponseEntity<ApiResponse<ReviewResponse>> moderarResena(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewModerationRequest request) {
        ReviewResponse response = reviewService.moderarResena(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Reseña moderada correctamente", response));
    }
}
