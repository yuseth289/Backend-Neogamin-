package com.neogaming.catalog.offer.dto.request;

import com.neogaming.common.enums.TipoDescuento;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO de entrada para crear o actualizar una oferta de descuento.
 *
 * Ejemplo de request body (descuento porcentual):
 * {
 *   "name":          "Black Friday 2026",
 *   "discountType":  "PERCENTAGE",
 *   "discountValue": 20.00,
 *   "startDate":     "2026-11-27T00:00:00Z",
 *   "endDate":       "2026-11-30T23:59:59Z"
 * }
 */
public record OfferRequest(

        @NotBlank(message = "El nombre de la oferta es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,

        @NotNull(message = "El tipo de descuento es obligatorio (PERCENTAGE o FIXED)")
        TipoDescuento discountType,

        @NotNull(message = "El valor del descuento es obligatorio")
        @DecimalMin(value = "0.01", message = "El valor del descuento debe ser mayor que 0")
        BigDecimal discountValue,

        @NotNull(message = "La fecha de inicio es obligatoria")
        Instant startDate,

        @NotNull(message = "La fecha de fin es obligatoria")
        Instant endDate
) {}
