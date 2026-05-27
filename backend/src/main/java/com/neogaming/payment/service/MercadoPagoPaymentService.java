package com.neogaming.payment.service;

import com.neogaming.checkout.domain.CheckoutItem;
import com.neogaming.common.enums.TipoPago;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Servicio de creación de preferencias de pago en Mercado Pago Colombia.
 *
 * Responsabilidades:
 *  - Crear preferencias de Checkout Pro con split de marketplace
 *    (marketplace_fee en COP calculada desde PLATFORM_FEE_PERCENTAGE).
 *  - Incluir X-Idempotency-Key UUID aleatorio en cada llamada.    [Rule 4]
 *  - Configurar payer.identification para PSE (requiere CC o NIT). [Colombia 2025]
 *  - Configurar payment_methods según el método seleccionado.
 *  - Usar back_urls desde application.yml.
 *
 * Nota: Este servicio complementa a MercadoPagoClient (que usa RestTemplate
 * directamente). Se enfoca en la lógica de negocio de marketplace. En una
 * refactorización futura ambos se pueden unificar.
 *
 * // Rule 12: Este servicio NUNCA almacena CVV, PAN ni track data.
 * // Rule 11: Solo se colecta el tipo de documento del comprador (requerido por PSE),
 * //          conforme a la Ley 1581 de protección de datos personales (Colombia).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoPaymentService {

    private static final String MP_API_BASE        = "https://api.mercadopago.com";
    private static final String PREFERENCES_PATH   = "/checkout/preferences";
    private static final String CURRENCY_COP        = "COP";

    private final RestTemplate restTemplate;

    // ── Configuración desde application.yml / variables de entorno ────────────

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Value("${mercadopago.sandbox:true}")
    private boolean sandbox;

    /** Porcentaje de comisión de la plataforma (ej: 5.0 = 5%) */
    @Value("${app.business.platform-fee-percentage:${PLATFORM_FEE_PERCENTAGE:5.0}}")
    private double platformFeePercentage;

    /** URL de retorno al pago aprobado */
    @Value("${mercadopago.back-urls.success:${app.base-url:http://localhost:4200}/checkout/result?status=success}")
    private String backUrlSuccess;

    /** URL de retorno al pago pendiente (PSE, Efecty) */
    @Value("${mercadopago.back-urls.pending:${app.base-url:http://localhost:4200}/checkout/result?status=pending}")
    private String backUrlPending;

    /** URL de retorno al pago rechazado o cancelado */
    @Value("${mercadopago.back-urls.failure:${app.base-url:http://localhost:4200}/checkout/result?status=failed}")
    private String backUrlFailure;

    /** URL del webhook de notificaciones (configurada en application.yml) */
    @Value("${app.base-url:http://localhost:8080}/webhooks/mercadopago")
    private String notificationUrl;

    /**
     * Crea una preferencia de Checkout Pro en Mercado Pago con split de marketplace.
     *
     * El {@code marketplace_fee} se calcula como porcentaje del total y se descuenta
     * automáticamente del monto que recibe el vendedor. NeoGaming retiene esa comisión.
     *
     * Para PSE se incluye {@code payer.identification} con tipo de documento,
     * requerido por MP Colombia para débito bancario en línea.
     *
     * @param checkoutId       UUID del checkout (usado como external_reference)
     * @param items            Ítems del checkout
     * @param total            Total a cobrar en COP
     * @param metodoPago       Método de pago seleccionado por el usuario
     * @param payerEmail       Email del comprador (requerido por MP)
     * @param payerDocType     Tipo de documento del comprador ("CC" o "NIT") — solo PSE
     * @param payerDocNumber   Número de documento del comprador — solo PSE
     * @return URL de checkout de MP (init_point o sandbox_init_point según el entorno)
     */
    public String crearPreferencia(
            String checkoutId,
            List<CheckoutItem> items,
            BigDecimal total,
            TipoPago metodoPago,
            String payerEmail,
            String payerDocType,
            String payerDocNumber) {

        Map<String, Object> body = new HashMap<>();

        // ── Ítems ──────────────────────────────────────────────────────────────
        List<Map<String, Object>> mpItems = new ArrayList<>();
        for (CheckoutItem item : items) {
            Map<String, Object> mpItem = new HashMap<>();
            mpItem.put("id", item.getProductId().toString());
            mpItem.put("title", "Producto NeoGaming");     // Rule 11: no incluir nombre completo del producto en preferencia
            mpItem.put("quantity", item.getQuantity());
            mpItem.put("unit_price", item.getUnitPrice());
            mpItem.put("currency_id", CURRENCY_COP);
            mpItems.add(mpItem);
        }
        body.put("items", mpItems);

        // ── Comisión de marketplace ────────────────────────────────────────────
        // marketplace_fee: monto en COP que retiene NeoGaming como plataforma.
        // MP lo descuenta del pago antes de transferirlo al vendedor.
        BigDecimal marketplaceFee = total
                .multiply(BigDecimal.valueOf(platformFeePercentage / 100.0))
                .setScale(2, RoundingMode.HALF_UP);
        body.put("marketplace_fee", marketplaceFee);

        // ── Información del pagador ────────────────────────────────────────────
        // Rule 11: Se colecta email y tipo de documento únicamente cuando es
        // requerido por el método de pago (PSE exige identification en Colombia).
        if (payerEmail != null && !payerEmail.isBlank()) {
            Map<String, Object> payer = new HashMap<>();
            payer.put("email", payerEmail);

            // PSE en Colombia requiere tipo y número de documento del titular de la cuenta bancaria
            if (metodoPago == TipoPago.MP_PSE && payerDocType != null && payerDocNumber != null) {
                Map<String, String> identification = new HashMap<>();
                // Tipos válidos en Colombia: "CC" (cédula de ciudadanía) o "NIT"
                identification.put("type", payerDocType);
                identification.put("number", payerDocNumber);
                payer.put("identification", identification);
            }
            body.put("payer", payer);
        }

        // ── URLs de retorno ────────────────────────────────────────────────────
        Map<String, String> backUrls = new HashMap<>();
        backUrls.put("success", backUrlSuccess);
        backUrls.put("pending", backUrlPending);
        backUrls.put("failure", backUrlFailure);
        body.put("back_urls", backUrls);
        body.put("auto_return", "approved");

        // ── Referencia externa ─────────────────────────────────────────────────
        body.put("external_reference", checkoutId);

        // ── URL de notificaciones webhook ──────────────────────────────────────
        body.put("notification_url", notificationUrl);

        // ── Métodos de pago ────────────────────────────────────────────────────
        configurarMetodoPago(body, metodoPago);

        log.info("Creando preferencia MP — checkoutId: {}, metodoPago: {}, fee: {} COP",
                checkoutId, metodoPago, marketplaceFee);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    MP_API_BASE + PREFERENCES_PATH,
                    HttpMethod.POST,
                    new HttpEntity<>(body, buildHeaders()),
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException("Respuesta vacía de Mercado Pago al crear preferencia");
            }

            String initPoint        = (String) responseBody.get("init_point");
            String sandboxInitPoint = (String) responseBody.get("sandbox_init_point");

            log.info("Preferencia MP creada — checkoutId: {}, preferenceId: {}",
                    checkoutId, responseBody.get("id"));

            return sandbox ? sandboxInitPoint : initPoint;

        } catch (HttpClientErrorException e) {
            log.error("Error HTTP al crear preferencia MP — checkoutId: {}, status: {}, body: {}",
                    checkoutId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Error al crear preferencia en Mercado Pago: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Error inesperado al crear preferencia MP — checkoutId: {}: {}", checkoutId, e.getMessage());
            throw new RuntimeException("Error al conectar con Mercado Pago", e);
        }
    }

    /**
     * Configura los métodos de pago disponibles según el tipo seleccionado.
     *
     * Para PSE: preselecciona débito bancario en línea.
     * Para Efecty: preselecciona pago en efectivo.
     * Para tarjetas: sin restricciones adicionales (MP muestra todas las opciones).
     */
    private void configurarMetodoPago(Map<String, Object> body, TipoPago metodoPago) {
        Map<String, Object> paymentMethods = new HashMap<>();

        switch (metodoPago) {
            case MP_PSE -> {
                // PSE: débito bancario en línea (Colombia)
                paymentMethods.put("default_payment_method_id", "pse");
                // Excluir efectivo para simplificar la experiencia
                paymentMethods.put("excluded_payment_types",
                        List.of(Map.of("id", "ticket")));
            }
            case MP_EFECTY -> {
                paymentMethods.put("default_payment_method_id", "efecty");
                paymentMethods.put("excluded_payment_types",
                        List.of(Map.of("id", "bank_transfer")));
            }
            case MP_NEQUI ->
                paymentMethods.put("default_payment_method_id", "nequi");
            case MP_CREDIT_CARD ->
                paymentMethods.put("excluded_payment_types",
                        List.of(Map.of("id", "debit_card"),
                                Map.of("id", "bank_transfer"),
                                Map.of("id", "ticket")));
            case MP_DEBIT_CARD ->
                paymentMethods.put("excluded_payment_types",
                        List.of(Map.of("id", "credit_card"),
                                Map.of("id", "bank_transfer"),
                                Map.of("id", "ticket")));
            default -> { /* MP_ACCOUNT_MONEY y otros: sin restricciones */ }
        }

        if (!paymentMethods.isEmpty()) {
            body.put("payment_methods", paymentMethods);
        }
    }

    /**
     * Construye los headers requeridos por la API de Mercado Pago.
     *
     * // Rule 4: X-Idempotency-Key con UUID.randomUUID() en cada llamada.
     * Esto garantiza que si MP recibe la misma petición dos veces (por reintento
     * de red), no crea dos preferencias distintas.
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        // Rule 4: UUID aleatorio — nunca reutilizar entre llamadas distintas a MP
        headers.set("X-Idempotency-Key", UUID.randomUUID().toString());
        return headers;
    }
}
