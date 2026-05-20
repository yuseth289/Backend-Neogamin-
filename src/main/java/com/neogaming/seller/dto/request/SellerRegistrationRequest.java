package com.neogaming.seller.dto.request;

import com.neogaming.common.enums.TipoDocumentoVendedor;
import com.neogaming.common.enums.TipoRegimenFiscal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para registrar un perfil de vendedor en NeoGaming.
 *
 * Al enviar este request, el usuario solicita convertirse en vendedor.
 * El perfil queda en estado PENDING hasta que un administrador lo apruebe.
 *
 * Ejemplo de request body:
 * {
 *   "storeName":        "Gaming Shop Bogotá",
 *   "storeDescription": "Tienda especializada en periféricos gaming",
 *   "tipoDocumento":    "NIT",
 *   "numeroDocumento":  "900123456-7",
 *   "razonSocial":      "Gaming Shop SAS",
 *   "tipoRegimen":      "RESPONSABLE_IVA",
 *   "phone":            "3001234567",
 *   "city":             "Bogotá",
 *   "department":       "Cundinamarca"
 * }
 */
public record SellerRegistrationRequest(

        @NotBlank(message = "El nombre de la tienda es obligatorio")
        @Size(max = 100, message = "El nombre de la tienda no puede superar los 100 caracteres")
        String storeName,

        @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres")
        String storeDescription,

        @NotNull(message = "El tipo de documento es obligatorio")
        TipoDocumentoVendedor tipoDocumento,

        @NotBlank(message = "El número de documento es obligatorio")
        @Size(max = 30, message = "El número de documento no puede superar los 30 caracteres")
        String numeroDocumento,

        /** Razón social — obligatoria cuando tipoDocumento = NIT */
        @Size(max = 200, message = "La razón social no puede superar los 200 caracteres")
        String razonSocial,

        @NotNull(message = "El tipo de régimen fiscal es obligatorio")
        TipoRegimenFiscal tipoRegimen,

        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String phone,

        @Size(max = 300, message = "La dirección no puede superar los 300 caracteres")
        String address,

        @Size(max = 100, message = "La ciudad no puede superar los 100 caracteres")
        String city,

        @Size(max = 100, message = "El departamento no puede superar los 100 caracteres")
        String department
) {}
