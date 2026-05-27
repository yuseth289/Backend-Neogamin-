package com.neogaming.seller.dto.response;

import com.neogaming.common.enums.EstadoGenerico;
import com.neogaming.common.enums.TipoDocumentoVendedor;
import com.neogaming.common.enums.TipoRegimenFiscal;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de salida con los datos del perfil de un vendedor.
 *
 * Se usa tanto para el vendedor viendo su propio perfil
 * como para el administrador revisando solicitudes.
 *
 * Nota: el campo mpAccessToken NO se incluye por seguridad.
 */
public record SellerResponse(

        UUID id,
        UUID userId,

        // Datos de la tienda
        String storeName,
        String storeSlug,
        String storeDescription,
        String storeLogoUrl,
        String storeBannerUrl,

        // Datos fiscales
        TipoDocumentoVendedor tipoDocumento,
        String numeroDocumento,
        String razonSocial,
        TipoRegimenFiscal tipoRegimen,

        // Contacto
        String phone,
        String address,
        String city,
        String department,

        // Integración MP (solo el user_id, nunca el token)
        String mpUserId,

        // Estado
        EstadoGenerico status,

        Instant createdAt,
        Instant updatedAt
) {}
