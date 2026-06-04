package com.neogaming.payment.webhook;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neogaming.payment.domain.WebhookEventProcessed;
import com.neogaming.payment.mercadopago.MercadoPagoSignatureVerifier;
import com.neogaming.payment.mercadopago.SignatureVerificationException;
import com.neogaming.payment.repository.WebhookEventProcessedRepository;
import com.neogaming.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;
import java.util.Enumeration;
import java.util.Map;

/**
 * Endpoint seguro para recibir webhooks de Mercado Pago Colombia.
 *
 * Ruta: POST /webhooks/mercadopago
 * (namespacing PagoKit: /webhooks/<provider>)
 *
 * Flujo de seguridad (en orden):
 *  1. Verificar Content-Length <= 256 KB para prevenir DoS.
 *  2. Leer el raw body del request ANTES de parsear JSON.        [Rule 5]
 *  3. Verificar firma HMAC-SHA256 con x-signature y el secret.  [Rule 3]
 *  4. Verificar ventana de tiempo (replay protection, 300 s).   [Rule 3]
 *  5. Deduplicar por x-request-id en webhook_events_processed.  [Rule 3]
 *  6. Parsear el body JSON y delegar al servicio de pagos.
 *
 * Solo se loguean event.id, event.type y event.created.        [Rule 11]
 * Nunca se almacena CVV, PAN ni track data.                    [Rule 12]
 *
 * Importante: este controller sustituye el handler /webhooks/mercadopago
 * que estaba en PaymentController, el cual no verificaba firma HMAC.
 */
@RestController
@RequestMapping("/webhooks/mercadopago")
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoWebhookController {

    // Rule 5: Límite de 256 KB en el body del webhook para prevenir ataques DoS
    private static final int MAX_BODY_BYTES = 256 * 1024;

    private static final String PROVIDER = "mercadopago";

    private final MercadoPagoSignatureVerifier signatureVerifier;
    private final WebhookEventProcessedRepository webhookRepo;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @Value("${mercadopago.webhook-secret:}")
    private String webhookSecret;

    /**
     * Recibe notificaciones de pago de Mercado Pago.
     *
     * MP envía las notificaciones con los headers:
     *  - {@code x-signature}:   {@code ts=<epoch>,v1=<hmac>}
     *  - {@code x-request-id}:  UUID único de la notificación (para deduplicar)
     *
     * Los datos del evento vienen en el body JSON y también como query params.
     * MP espera siempre HTTP 200 como ACK; si no lo recibe, reintenta.
     *
     * @param request HttpServletRequest para leer raw body antes de parsear JSON
     * @return 200 OK si el evento se procesó (o se descartó legítimamente)
     *         401 si la firma es inválida
     *         413 si el body supera 256 KB
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receiveWebhook(HttpServletRequest request) {

        // ── 1. Verificar Content-Length (DoS protection) ───────────────────────
        // Rule 5: Rechazar body > 256 KB antes de leer
        int contentLength = request.getContentLength();
        if (contentLength > MAX_BODY_BYTES) {
            log.warn("Webhook MP rechazado — body supera 256 KB ({} bytes)", contentLength);
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        // ── 2. Extraer headers de seguridad ────────────────────────────────────
        String xSignature = request.getHeader("x-signature");
        String xRequestId = request.getHeader("x-request-id");

        // Diagnóstico temporal — ver qué query params y headers llegan de MP
        Enumeration<String> paramNames = request.getParameterNames();
        StringBuilder paramLog = new StringBuilder();
        while (paramNames.hasMoreElements()) {
            String p = paramNames.nextElement();
            paramLog.append(p).append("=").append(request.getParameter(p)).append(" ");
        }
        log.info("Webhook MP diag — query-params: [{}] | x-signature present: {} | x-request-id: {}",
                paramLog.toString().trim(), xSignature != null, xRequestId);

        // El ID de la notificación llega como query param — MP puede enviarlo como "id" o "data.id"
        String notificationId = request.getParameter("id");
        if (notificationId == null || notificationId.isBlank()) {
            notificationId = request.getParameter("data.id");
        }
        if (notificationId == null || notificationId.isBlank()) {
            notificationId = "unknown";
        }
        log.info("Webhook MP diag — notificationId usado en manifest: [{}]", notificationId);

        // ── 3. Leer raw body ANTES de parsear JSON ─────────────────────────────
        // Rule 5: raw body capturado antes de cualquier parsing
        byte[] rawBody;
        try {
            rawBody = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        } catch (IOException e) {
            log.error("Error leyendo body del webhook MP: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Doble verificación de tamaño después de leer (por si Content-Length no vino)
        if (rawBody.length > MAX_BODY_BYTES) {
            log.warn("Webhook MP rechazado — body leído supera 256 KB");
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        // ── 4. Verificar firma HMAC-SHA256 ─────────────────────────────────────
        // Rule 3: Verificar firma ANTES de parsear payload JSON
        try {
            signatureVerifier.verify(xSignature, xRequestId, notificationId, webhookSecret);
        } catch (SignatureVerificationException e) {
            // No loguear el body ni la firma recibida para evitar filtraciones
            log.warn("Webhook MP — firma inválida: {} | x-request-id: {}", e.getMessage(), xRequestId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // ── 5. Deduplicar por x-request-id ────────────────────────────────────
        // Rule 3: Replay protection — descartar si ya procesamos este evento
        if (xRequestId != null && webhookRepo.existsByProviderAndEventId(PROVIDER, xRequestId)) {
            log.info("Webhook MP duplicado descartado — x-request-id: {}", xRequestId);
            return ResponseEntity.ok().build(); // ACK a MP para que no reintente
        }

        // ── 6. Parsear el body JSON ────────────────────────────────────────────
        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(rawBody, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Webhook MP — body JSON inválido: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Extraer tipo de evento y datos
        String action  = (String) payload.getOrDefault("action", "");
        Object dataObj = payload.get("data");
        String mpId    = null;
        if (dataObj instanceof Map<?, ?> dataMap) {
            Object idObj = dataMap.get("id");
            mpId = idObj != null ? idObj.toString() : null;
        }

        // Rule 11: Loguear solo event.id, event.type y timestamp — nunca PII ni el payload completo
        log.info("Webhook MP — action: {}, notificationId: {}, x-request-id: {}", action, notificationId, xRequestId);

        // ── 7. Procesar según el tipo de evento ────────────────────────────────
        try {
            if ("payment.created".equals(action) || "payment.updated".equals(action)) {
                if (mpId != null && !mpId.isBlank()) {
                    String topic = "payment";
                    paymentService.procesarWebhook(mpId, topic);
                } else {
                    log.warn("Webhook MP — action '{}' sin data.id válido", action);
                }
            } else {
                // TODO: implementar en MercadoPagoWebhookController si se necesita:
                // "merchant_order.created", "merchant_order.updated", "point.integration-terminal"
                log.info("Webhook MP — action '{}' no manejado, descartado", action);
            }

            // ── 8. Registrar evento como procesado (deduplicación) ─────────────
            if (xRequestId != null && !xRequestId.isBlank()) {
                WebhookEventProcessed processed = WebhookEventProcessed.builder()
                        .provider(PROVIDER)
                        .eventId(xRequestId)
                        .processedAt(Instant.now())
                        .build();
                webhookRepo.save(processed);
            }

        } catch (Exception e) {
            // Loguear el error pero responder 200 OK para que MP no reintente infinitamente.
            // Los errores de negocio (pago no encontrado, estado inválido) no deben causar reintentos.
            log.error("Error procesando webhook MP — x-request-id: {}, error: {}",
                    xRequestId, e.getMessage(), e);
        }

        return ResponseEntity.ok().build();
    }
}
