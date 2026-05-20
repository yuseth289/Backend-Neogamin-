package com.neogaming.payment.controller;

import com.neogaming.common.response.ApiResponse;
import com.neogaming.common.util.SecurityUtils;
import com.neogaming.payment.dto.response.PaymentResponse;
import com.neogaming.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para la gestión de pagos con Mercado Pago.
 *
 * Endpoints autenticados:
 *  POST /payments/checkout/{checkoutId}  → Iniciar pago de un checkout
 *
 * Endpoints públicos (webhook de MP):
 *  POST /webhooks/mercadopago            → Recibir notificaciones de pago de MP
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pagos", description = "Gestión de pagos con Mercado Pago Colombia")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Inicia el pago de un checkout mediante Mercado Pago.
     *
     * Retorna la URL de checkout de MP a la que el frontend debe redirigir al usuario.
     * El usuario completa el pago en la interfaz de Mercado Pago.
     *
     * @param checkoutId UUID del checkout a pagar
     * @return Respuesta con la URL de checkout y el ID del pago creado
     */
    @PostMapping("/payments/checkout/{checkoutId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Iniciar pago",
            description = "Crea una preferencia en MP y retorna la URL de checkout para redirigir al usuario."
    )
    public ResponseEntity<ApiResponse<PaymentResponse>> iniciarPago(
            @PathVariable UUID checkoutId) {
        PaymentResponse response = paymentService.iniciarPago(checkoutId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    /**
     * Endpoint de webhook para recibir notificaciones de pago de Mercado Pago.
     *
     * MP llama a este endpoint cuando ocurre un evento en un pago.
     * Los parámetros llegane como query params en la URL.
     *
     * Se responde siempre 200 OK para que MP no reintente la notificación,
     * incluso si el procesamiento interno falla (se loguea el error).
     *
     * URL configurada en MP como: https://api.neogaming.co/webhooks/mercadopago
     *
     * @param topic     Tipo de notificación ("payment", "merchant_order", etc.)
     * @param id        ID del recurso que generó la notificación (mpPaymentId)
     */
    @PostMapping("/webhooks/mercadopago")
    @Operation(
            summary = "Webhook de Mercado Pago",
            description = "Endpoint interno para recibir notificaciones de pago de Mercado Pago."
    )
    public ResponseEntity<Void> webhook(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String id) {

        log.info("Webhook MP recibido — topic: {}, id: {}", topic, id);

        try {
            if (id != null && !id.isBlank()) {
                paymentService.procesarWebhook(id, topic != null ? topic : "payment");
            }
        } catch (Exception e) {
            // Loguear pero responder 200 OK para evitar reintentos de MP
            log.error("Error procesando webhook MP — id: {}, error: {}", id, e.getMessage(), e);
        }

        // Siempre 200 OK para Mercado Pago
        return ResponseEntity.ok().build();
    }
}
