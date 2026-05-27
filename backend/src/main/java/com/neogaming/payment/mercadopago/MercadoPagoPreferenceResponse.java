package com.neogaming.payment.mercadopago;

/**
 * Respuesta de Mercado Pago al crear una preferencia de pago.
 *
 * - id:             ID de la preferencia en MP (se guarda como mpPreferenceId)
 * - initPoint:      URL de checkout de producción — redirigir al usuario aquí
 * - sandboxInitPoint: URL de checkout de sandbox — usar en desarrollo/pruebas
 */
public record MercadoPagoPreferenceResponse(
        String id,
        String initPoint,
        String sandboxInitPoint
) {}
