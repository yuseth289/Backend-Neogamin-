package com.neogaming.payment.mercadopago;

import com.neogaming.checkout.domain.CheckoutItem;
import com.neogaming.common.enums.TipoPago;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cliente HTTP para la API de Mercado Pago Colombia.
 *
 * Responsabilidades:
 *  - Crear preferencias de pago (checkout de MP)
 *  - Consultar el estado de un pago por su ID
 *  - Procesar reembolsos
 *
 * Documentación oficial de MP:
 *  https://www.mercadopago.com.co/developers/es/reference
 *
 * En desarrollo, se puede usar el sandbox de MP con credenciales de prueba.
 * Configurar en application.yml:
 *   mercadopago.access-token: APP_USR-...
 *   mercadopago.webhook-secret: tu-secret
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoClient {

    private final RestTemplate restTemplate;

    /** URL base de la API de Mercado Pago */
    private static final String MP_API_BASE = "https://api.mercadopago.com";

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Value("${app.base-url:http://localhost:8080}/webhooks/mercadopago")
    private String webhookUrl;

    @Value("${mercadopago.back-urls.success}")
    private String successUrl;

    @Value("${mercadopago.back-urls.pending}")
    private String pendingUrl;

    @Value("${mercadopago.back-urls.failure}")
    private String failureUrl;

    /**
     * Crea una preferencia de pago en Mercado Pago.
     *
     * La preferencia define los ítems, el monto total y las URLs de retorno.
     * MP devuelve una URL de checkout (init_point) a la que se redirige al usuario.
     *
     * @param checkoutId UUID del checkout (usado como external_reference)
     * @param items      Lista de ítems del checkout
     * @param total      Total a cobrar en COP
     * @param metodoPago Método de pago seleccionado
     * @return Respuesta de MP con el ID de preferencia y la URL de checkout
     */
    public MercadoPagoPreferenceResponse crearPreferencia(
            String checkoutId,
            List<CheckoutItem> items,
            BigDecimal total,
            TipoPago metodoPago) {

        Map<String, Object> body = new HashMap<>();

        // Ítems de la preferencia
        List<Map<String, Object>> mpItems = new ArrayList<>();
        items.forEach(item -> {
            Map<String, Object> mpItem = new HashMap<>();
            mpItem.put("id", item.getProductId().toString());
            mpItem.put("title", "Producto NeoGaming");
            mpItem.put("quantity", item.getQuantity());
            mpItem.put("unit_price", item.getUnitPrice().longValue()); // COP: entero, sin decimales
            mpItem.put("currency_id", "COP");
            mpItems.add(mpItem);
        });
        body.put("items", mpItems);

        // URLs de retorno al frontend tras el pago
        Map<String, String> backUrls = new HashMap<>();
        backUrls.put("success", successUrl);
        backUrls.put("pending", pendingUrl);
        backUrls.put("failure", failureUrl);
        body.put("back_urls", backUrls);
        // auto_return requiere URLs públicamente accesibles — solo habilitar en producción con HTTPS

        // Referencia externa para vincular el pago con el checkout
        body.put("external_reference", checkoutId);

        body.put("notification_url", webhookUrl);

        // Método de pago excluido/incluido según la selección
        configurarMetodoPago(body, metodoPago);

        log.info("Creando preferencia MP — checkout: {}, back_urls: success={}, pending={}, failure={}",
                checkoutId, successUrl, pendingUrl, failureUrl);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    MP_API_BASE + "/checkout/preferences",
                    HttpMethod.POST,
                    new HttpEntity<>(body, buildHeaders()),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> responseBody = response.getBody();
            return new MercadoPagoPreferenceResponse(
                    (String) responseBody.get("id"),
                    (String) responseBody.get("init_point"),
                    (String) responseBody.get("sandbox_init_point")
            );
        } catch (Exception e) {
            log.error("Error al crear preferencia en MP para checkout {}: {}", checkoutId, e.getMessage());
            throw new RuntimeException("Error al conectar con Mercado Pago: " + e.getMessage(), e);
        }
    }

    /**
     * Consulta el estado actual de un pago en Mercado Pago por su ID.
     * Usado para verificar el estado cuando llega un webhook.
     *
     * @param mpPaymentId ID del pago en Mercado Pago
     * @return Mapa con los datos del pago retornados por MP
     */
    public Map<String, Object> consultarPago(String mpPaymentId) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    MP_API_BASE + "/v1/payments/" + mpPaymentId,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error al consultar pago {} en MP: {}", mpPaymentId, e.getMessage());
            throw new RuntimeException("Error al consultar el pago en Mercado Pago", e);
        }
    }

    /**
     * Procesa un reembolso total de un pago en Mercado Pago.
     *
     * @param mpPaymentId ID del pago a reembolsar
     */
    public void procesarReembolso(String mpPaymentId) {
        try {
            restTemplate.exchange(
                    MP_API_BASE + "/v1/payments/" + mpPaymentId + "/refunds",
                    HttpMethod.POST,
                    new HttpEntity<>(new HashMap<>(), buildHeaders()),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            log.info("Reembolso procesado en MP para pago: {}", mpPaymentId);
        } catch (Exception e) {
            log.error("Error al procesar reembolso en MP para pago {}: {}", mpPaymentId, e.getMessage());
            throw new RuntimeException("Error al procesar el reembolso en Mercado Pago", e);
        }
    }

    /**
     * Configura los métodos de pago en la preferencia según el tipo seleccionado.
     * Excluye los demás métodos para simplificar la experiencia del usuario.
     *
     * @param body       Mapa del body de la preferencia
     * @param metodoPago Método de pago seleccionado por el usuario
     */
    private void configurarMetodoPago(Map<String, Object> body, TipoPago metodoPago) {
        Map<String, Object> paymentMethods = new HashMap<>();

        // Configurar según el método seleccionado
        switch (metodoPago) {
            case MP_PSE -> {
                List<Map<String, String>> excluded = new ArrayList<>();
                // PSE es débito bancario en línea — excluir efectivo y tarjetas
                paymentMethods.put("excluded_payment_types", excluded);
                paymentMethods.put("default_payment_method_id", "pse");
            }
            case MP_NEQUI -> paymentMethods.put("default_payment_method_id", "nequi");
            case MP_EFECTY -> paymentMethods.put("default_payment_method_id", "efecty");
            default -> { /* Tarjetas y saldo MP: sin restricciones */ }
        }

        if (!paymentMethods.isEmpty()) {
            body.put("payment_methods", paymentMethods);
        }
    }

    /**
     * Construye los headers HTTP requeridos por la API de Mercado Pago,
     * incluyendo un UUID aleatorio como X-Idempotency-Key.
     *
     * // Rule 4: X-Idempotency-Key con UUID aleatorio en cada POST a MP
     * para prevenir pagos duplicados ante reintentos de red.
     *
     * @return HttpHeaders con Content-Type, Authorization e X-Idempotency-Key configurados
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        // Rule 4: UUID aleatorio como idempotency key — nunca reutilizar entre llamadas distintas
        headers.set("X-Idempotency-Key", UUID.randomUUID().toString());
        return headers;
    }
}
