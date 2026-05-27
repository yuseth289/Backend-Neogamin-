package com.neogaming.checkout.dto.request;

import com.neogaming.common.enums.TipoPago;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO de entrada para iniciar un checkout.
 *
 * El usuario indica con qué dirección quiere recibir el pedido
 * y qué método de pago desea usar.
 *
 * Ejemplo de request body:
 * {
 *   "addressId":    "uuid-de-la-direccion",
 *   "paymentMethod": "MP_PSE"
 * }
 */
public record InitCheckoutRequest(

        @NotNull(message = "La dirección de entrega es obligatoria")
        UUID addressId,

        @NotNull(message = "El método de pago es obligatorio")
        TipoPago paymentMethod
) {}
