package com.neogaming.catalog.product.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de entrada para crear o actualizar un producto.
 * Usado tanto en POST (crear) como en PUT (actualizar).
 *
 * Ejemplo de request body:
 * {
 *   "name":        "Teclado Mecánico Logitech G Pro X",
 *   "description": "Teclado mecánico TKL para gaming profesional",
 *   "brand":       "Logitech",
 *   "sku":         "LGT-GPX-001",
 *   "categoryId":  "uuid-de-teclados",
 *   "basePrice":   350000.00,
 *   "ivaPercent":  19.00
 * }
 */
public record ProductRequest(

        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 200, message = "El nombre no puede superar los 200 caracteres")
        String name,

        @Size(max = 10000, message = "La descripción no puede superar los 10000 caracteres")
        String description,

        @Size(max = 100, message = "La marca no puede superar los 100 caracteres")
        String brand,

        @Size(max = 100, message = "El SKU no puede superar los 100 caracteres")
        String sku,

        /** UUID de la categoría. Puede ser null si no aplica categoría. */
        UUID categoryId,

        @NotNull(message = "El precio base es obligatorio")
        @DecimalMin(value = "0.0", message = "El precio base no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "El precio base tiene formato inválido")
        BigDecimal basePrice,

        /**
         * Porcentaje de IVA. Por defecto 19% (DIAN Colombia).
         * Usar 0 para productos exentos de IVA.
         */
        @DecimalMin(value = "0.0", message = "El IVA no puede ser negativo")
        @DecimalMax(value = "100.0", message = "El IVA no puede superar el 100%")
        BigDecimal ivaPercent
) {}
