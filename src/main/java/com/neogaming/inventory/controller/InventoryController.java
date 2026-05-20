package com.neogaming.inventory.controller;

import com.neogaming.common.response.ApiResponse;
import com.neogaming.common.response.PageResponse;
import com.neogaming.common.util.SecurityUtils;
import com.neogaming.inventory.dto.request.StockAdjustRequest;
import com.neogaming.inventory.dto.response.InventoryMovementResponse;
import com.neogaming.inventory.dto.response.InventoryResponse;
import com.neogaming.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para la gestión de inventario del vendedor.
 *
 * Todos los endpoints requieren rol SELLER.
 *
 * Endpoints disponibles:
 *  GET   /inventory/{productId}             → Ver estado del inventario
 *  POST  /inventory/{productId}/stock       → Agregar stock (entrada de mercancía)
 *  PATCH /inventory/{productId}/stock       → Ajustar stock (corrección absoluta)
 *  GET   /inventory/{productId}/movements   → Historial de movimientos
 */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Inventario", description = "Gestión de stock de productos del vendedor")
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Retorna el estado actual del inventario de un producto.
     * Incluye physicalStock, reservedStock y availableStock calculado.
     */
    @GetMapping("/{productId}")
    @Operation(
            summary = "Ver inventario del producto",
            description = "Retorna el stock físico, reservado y disponible del producto."
    )
    public ResponseEntity<ApiResponse<InventoryResponse>> obtener(@PathVariable UUID productId) {
        InventoryResponse response = inventoryService.obtenerPorProducto(productId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Agrega stock físico al producto (registra entrada de mercancía).
     * El quantity se suma al stock actual.
     */
    @PostMapping("/{productId}/stock")
    @Operation(
            summary = "Agregar stock",
            description = "Agrega unidades al stock físico del producto. Registra un movimiento de tipo INITIAL_LOAD."
    )
    public ResponseEntity<ApiResponse<InventoryResponse>> agregarStock(
            @PathVariable UUID productId,
            @Valid @RequestBody StockAdjustRequest request) {
        InventoryResponse response = inventoryService.agregarStock(
                productId, request, SecurityUtils.getCurrentUserId()
        );
        return ResponseEntity.ok(ApiResponse.ok("Stock actualizado correctamente", response));
    }

    /**
     * Ajusta el stock físico a un valor absoluto (corrección por conteo físico).
     * El quantity reemplaza el stock actual, no se suma.
     */
    @PatchMapping("/{productId}/stock")
    @Operation(
            summary = "Ajustar stock",
            description = "Establece el stock físico a un valor absoluto. Registra un movimiento de tipo MANUAL_ADJUSTMENT."
    )
    public ResponseEntity<ApiResponse<InventoryResponse>> ajustarStock(
            @PathVariable UUID productId,
            @Valid @RequestBody StockAdjustRequest request) {
        InventoryResponse response = inventoryService.ajustarStock(
                productId, request, SecurityUtils.getCurrentUserId()
        );
        return ResponseEntity.ok(ApiResponse.ok("Stock ajustado correctamente", response));
    }

    /**
     * Retorna el historial completo de movimientos de inventario del producto.
     * Ordenado del más reciente al más antiguo.
     */
    @GetMapping("/{productId}/movements")
    @Operation(
            summary = "Historial de movimientos",
            description = "Lista todos los cambios de stock del producto con paginación."
    )
    public ResponseEntity<ApiResponse<PageResponse<InventoryMovementResponse>>> listarMovimientos(
            @PathVariable UUID productId,
            @PageableDefault(size = 30) Pageable pageable) {
        PageResponse<InventoryMovementResponse> page = inventoryService.listarMovimientos(
                productId, SecurityUtils.getCurrentUserId(), pageable
        );
        return ResponseEntity.ok(ApiResponse.ok(page));
    }
}
