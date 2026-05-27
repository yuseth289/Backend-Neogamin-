package com.neogaming.catalog.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para agregar una imagen a un producto.
 *
 * En producción, el flujo típico es:
 * 1. Frontend solicita una presigned URL a S3
 * 2. Frontend sube la imagen directamente a S3
 * 3. Frontend envía la URL resultante en este request
 */
public record ProductImageRequest(

        @NotBlank(message = "La URL de la imagen es obligatoria")
        @Size(max = 500, message = "La URL no puede superar los 500 caracteres")
        String url,

        @Size(max = 200, message = "El texto alternativo no puede superar los 200 caracteres")
        String altText,

        /** Orden de visualización en la galería (0 = primera). Por defecto 0. */
        int sortOrder
) {}
