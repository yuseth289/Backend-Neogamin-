package com.neogaming.invoice.controller;

import com.neogaming.common.response.ApiResponse;
import com.neogaming.common.response.PageResponse;
import com.neogaming.common.util.SecurityUtils;
import com.neogaming.invoice.dto.CancelInvoiceRequest;
import com.neogaming.invoice.dto.InvoiceResponse;
import com.neogaming.invoice.service.InvoiceService;
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
 * Controlador REST para la gestión de facturas electrónicas en NeoGaming.
 *
 * Endpoints del comprador (requieren JWT):
 *  GET  /invoices                       → Mis facturas
 *  GET  /invoices/order/{orderId}       → Factura de una orden específica
 *
 * Endpoints de administración (requieren rol ADMIN):
 *  GET    /admin/invoices               → Todas las facturas paginadas
 *  GET    /admin/invoices/{id}          → Cualquier factura por ID
 *  PATCH  /admin/invoices/{id}/cancel   → Anular una factura
 */
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Facturas", description = "Consulta y gestión de facturas electrónicas")
public class InvoiceController {

    private final InvoiceService invoiceService;

    // ===== ENDPOINTS DEL COMPRADOR =====

    /**
     * Lista todas las facturas del comprador autenticado, de más reciente a más antigua.
     */
    @GetMapping("/invoices")
    @Operation(
            summary = "Mis facturas",
            description = "Retorna todas las facturas del comprador autenticado de forma paginada."
    )
    public ResponseEntity<ApiResponse<PageResponse<InvoiceResponse>>> listarMisFacturas(
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<InvoiceResponse> page = PageResponse.from(
                invoiceService.listarMisFacturas(SecurityUtils.getCurrentUserId(), pageable));
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    /**
     * Retorna la factura correspondiente a una orden específica del comprador.
     */
    @GetMapping("/invoices/order/{orderId}")
    @Operation(
            summary = "Factura de una orden",
            description = "Retorna la factura electrónica asociada a la orden indicada. " +
                          "Solo el comprador de la orden puede consultarla."
    )
    public ResponseEntity<ApiResponse<InvoiceResponse>> obtenerFacturaPorOrden(
            @PathVariable UUID orderId) {
        InvoiceResponse response = invoiceService.obtenerPorOrden(
                orderId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ===== ENDPOINTS DE ADMINISTRACIÓN =====

    /**
     * Lista todas las facturas del sistema de forma paginada.
     */
    @GetMapping("/admin/invoices")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Todas las facturas (Admin)",
            description = "Retorna todas las facturas de la plataforma paginadas. Solo administradores."
    )
    public ResponseEntity<ApiResponse<PageResponse<InvoiceResponse>>> listarTodas(
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<InvoiceResponse> page = PageResponse.from(
                invoiceService.listarTodas(pageable));
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    /**
     * Retorna cualquier factura por su ID.
     */
    @GetMapping("/admin/invoices/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Detalle de factura (Admin)",
            description = "Retorna el detalle completo de cualquier factura. Solo administradores."
    )
    public ResponseEntity<ApiResponse<InvoiceResponse>> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.obtenerPorId(id)));
    }

    /**
     * Anula una factura indicando el motivo. Solo administradores.
     */
    @PatchMapping("/admin/invoices/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Anular factura (Admin)",
            description = "Marca la factura como CANCELLED con el motivo indicado. " +
                          "No se puede anular una factura que ya está cancelada."
    )
    public ResponseEntity<ApiResponse<InvoiceResponse>> cancelarFactura(
            @PathVariable UUID id,
            @Valid @RequestBody CancelInvoiceRequest request) {
        InvoiceResponse response = invoiceService.cancelarFactura(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Factura anulada correctamente", response));
    }
}
