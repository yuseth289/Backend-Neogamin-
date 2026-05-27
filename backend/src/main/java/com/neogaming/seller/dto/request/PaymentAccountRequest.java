package com.neogaming.seller.dto.request;

import com.neogaming.common.enums.TipoCuentaPago;
import com.neogaming.common.enums.TipoDocumentoVendedor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para registrar una cuenta bancaria del vendedor.
 *
 * Esta cuenta se usa para recibir el pago del split de Mercado Pago
 * después de cada venta completada.
 *
 * Ejemplo de request body:
 * {
 *   "bankName":       "Bancolombia",
 *   "accountType":    "AHORROS",
 *   "accountNumber":  "12345678901",
 *   "accountHolder":  "Juan García",
 *   "documentType":   "CC",
 *   "documentNumber": "1020304050"
 * }
 */
public record PaymentAccountRequest(

        @NotBlank(message = "El nombre del banco es obligatorio")
        @Size(max = 100, message = "El nombre del banco no puede superar los 100 caracteres")
        String bankName,

        @NotNull(message = "El tipo de cuenta es obligatorio (AHORROS o CORRIENTE)")
        TipoCuentaPago accountType,

        @NotBlank(message = "El número de cuenta es obligatorio")
        @Size(max = 30, message = "El número de cuenta no puede superar los 30 caracteres")
        String accountNumber,

        @NotBlank(message = "El nombre del titular es obligatorio")
        @Size(max = 200, message = "El nombre del titular no puede superar los 200 caracteres")
        String accountHolder,

        @NotNull(message = "El tipo de documento del titular es obligatorio")
        TipoDocumentoVendedor documentType,

        @NotBlank(message = "El número de documento del titular es obligatorio")
        @Size(max = 30, message = "El número de documento no puede superar los 30 caracteres")
        String documentNumber
) {}
