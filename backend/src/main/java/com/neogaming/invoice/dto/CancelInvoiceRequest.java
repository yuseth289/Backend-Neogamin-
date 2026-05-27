package com.neogaming.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de anulación de una factura.
 * Solo administradores pueden anular facturas.
 */
public record CancelInvoiceRequest(
        @NotBlank(message = "El motivo de anulación es obligatorio")
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String cancelReason
) {}
