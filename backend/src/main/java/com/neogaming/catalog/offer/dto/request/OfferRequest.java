package com.neogaming.catalog.offer.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de entrada para crear una oferta de descuento porcentual.
 *
 * Ejemplo de request body:
 * {
 *   "discountPercent": 20,
 *   "startDate":       "2026-11-27",
 *   "endDate":         "2026-11-30"
 * }
 */
public record OfferRequest(

        @NotNull(message = "El porcentaje de descuento es obligatorio")
        @DecimalMin(value = "1", message = "El descuento debe ser al menos 1%")
        @DecimalMax(value = "90", message = "El descuento no puede superar el 90%")
        BigDecimal discountPercent,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate startDate,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate endDate
) {}
