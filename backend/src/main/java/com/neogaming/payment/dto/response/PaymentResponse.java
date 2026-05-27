package com.neogaming.payment.dto.response;

import com.neogaming.common.enums.EstadoPago;
import com.neogaming.common.enums.TipoPago;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de salida con los datos de un pago.
 *
 * El campo mpPaymentId se devuelve para que el frontend pueda
 * consultar el estado directamente en Mercado Pago si lo necesita.
 * El campo mpResponse (raw de MP) NO se incluye por seguridad.
 */
public record PaymentResponse(
        UUID id,
        UUID checkoutId,
        String mpPaymentId,
        String mpPreferenceId,
        /** URL de checkout de MP — el frontend redirige aquí al usuario */
        String checkoutUrl,
        EstadoPago status,
        TipoPago paymentMethod,
        BigDecimal amount,
        Instant createdAt
) {}
