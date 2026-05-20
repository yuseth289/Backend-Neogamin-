package com.neogaming.seller.dto.response;

import com.neogaming.common.enums.TipoCuentaPago;
import com.neogaming.common.enums.TipoDocumentoVendedor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de salida con los datos de una cuenta bancaria del vendedor.
 *
 * El número de cuenta se devuelve enmascarado por seguridad
 * (solo los últimos 4 dígitos visibles). El enmascaramiento
 * se aplica en el mapper, no en este DTO.
 */
public record PaymentAccountResponse(

        UUID id,

        String bankName,
        TipoCuentaPago accountType,

        /** Número de cuenta enmascarado, ej: "****7890" */
        String accountNumberMasked,

        String accountHolder,
        TipoDocumentoVendedor documentType,
        String documentNumber,

        /** true si es la cuenta activa para recibir pagos */
        boolean active,

        Instant createdAt
) {}
