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
 * Nota: el endpoint de webhook POST /webhooks/mercadopago fue movido a
 * {@link com.neogaming.payment.webhook.MercadoPagoWebhookController}
 * para incluir verificación HMAC-SHA256, replay protection y body size cap.
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

    /** Solo sandbox — simula aprobación de pago para probar el flujo de orden. */
    @PostMapping("/payments/test/approve/{checkoutId}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> simularAprobacion(@PathVariable UUID checkoutId) {
        paymentService.simularPagoAprobado(checkoutId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

}
