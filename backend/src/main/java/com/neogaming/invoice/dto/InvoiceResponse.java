package com.neogaming.invoice.dto;

import com.neogaming.common.enums.EstadoFactura;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO de salida con los datos completos de una factura.
 * Expuesto al comprador y al administrador.
 */
public record InvoiceResponse(
        UUID id,
        UUID orderId,
        String invoiceNumber,
        EstadoFactura status,
        String buyerName,
        String buyerEmail,
        String buyerDocument,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal total,
        List<InvoiceItemResponse> items,
        Instant issuedAt,
        Instant cancelledAt,
        String cancelReason,
        Instant createdAt
) {}
