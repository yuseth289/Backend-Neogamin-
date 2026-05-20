package com.neogaming.invoice.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de salida con los datos de un ítem dentro de una factura.
 */
public record InvoiceItemResponse(
        UUID productId,
        String productName,
        String productSku,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
