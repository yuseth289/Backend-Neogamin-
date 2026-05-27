package com.neogaming.payment.mercadopago;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Verifica la firma HMAC-SHA256 de los webhooks de Mercado Pago (Colombia, 2025+).
 *
 * Algoritmo oficial de MP:
 *   manifest = "id:<notification_id>;request-id:<x-request-id>;ts:<ts>;"
 *   signature = HMAC-SHA256(MP_WEBHOOK_SECRET, manifest)
 *
 * El header x-signature llega con el formato:
 *   ts=<epoch_segundos>,v1=<hex_hmac>
 *
 * Tolerancia de tiempo: 300 segundos.
 *
 * Referencia: https://www.mercadopago.com.co/developers/es/docs/your-integrations/notifications/webhooks
 *
 * // Rule 3: La verificación de firma ocurre ANTES de parsear el payload JSON.
 */
@Component
@Slf4j
public class MercadoPagoSignatureVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /** Ventana de tolerancia en segundos para el replay protection. */
    // Rule 3: Replay protection — rechazar eventos con timestamp > 300 s de diferencia
    static final long MAX_CLOCK_SKEW_SECONDS = 300L;

    /**
     * Verifica la firma del webhook recibido de Mercado Pago.
     *
     * @param xSignature     Valor del header {@code x-signature} (formato: {@code ts=...,v1=...})
     * @param xRequestId     Valor del header {@code x-request-id} (UUID enviado por MP)
     * @param notificationId ID de la notificación (query param {@code id} en el webhook)
     * @param secret         {@code MP_WEBHOOK_SECRET} configurado en el dashboard de MP
     * @throws SignatureVerificationException si la firma es inválida, el timestamp expiró
     *                                        o los headers están malformados
     */
    public void verify(String xSignature,
                       String xRequestId,
                       String notificationId,
                       String secret) throws SignatureVerificationException {

        if (xSignature == null || xSignature.isBlank()) {
            // Rule 3: Rechazar webhook sin header x-signature
            throw new SignatureVerificationException("Header x-signature ausente");
        }
        if (xRequestId == null || xRequestId.isBlank()) {
            throw new SignatureVerificationException("Header x-request-id ausente");
        }
        if (secret == null || secret.isBlank()) {
            throw new SignatureVerificationException(
                    "MP_WEBHOOK_SECRET no configurado — configura la variable de entorno");
        }

        // ── 1. Extraer ts y v1 del header x-signature ─────────────────────────
        long ts = -1;
        String receivedHmac = null;

        for (String part : xSignature.split(",")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("ts=")) {
                try {
                    ts = Long.parseLong(trimmed.substring(3));
                } catch (NumberFormatException e) {
                    throw new SignatureVerificationException(
                            "Formato inválido de ts en x-signature: " + trimmed);
                }
            } else if (trimmed.startsWith("v1=")) {
                receivedHmac = trimmed.substring(3);
            }
        }

        if (ts < 0 || receivedHmac == null) {
            throw new SignatureVerificationException(
                    "x-signature malformado — se esperaba 'ts=...,v1=...'");
        }

        // ── 2. Validar ventana de tiempo (replay protection) ───────────────────
        // Rule 3: Rechazar si el timestamp supera los 300 s de diferencia
        long nowEpoch = Instant.now().getEpochSecond();
        if (Math.abs(nowEpoch - ts) > MAX_CLOCK_SKEW_SECONDS) {
            throw new SignatureVerificationException(
                    "Timestamp del webhook fuera de la ventana de 300 s — posible replay attack");
        }

        // ── 3. Construir el manifest y calcular HMAC ───────────────────────────
        // Formato oficial MP: "id:<notification_id>;request-id:<x-request-id>;ts:<ts>;"
        String manifest = "id:" + notificationId +
                          ";request-id:" + xRequestId +
                          ";ts:" + ts + ";";

        String computedHmac = computeHmacSha256(secret, manifest);

        // ── 4. Comparación en tiempo constante para evitar timing attacks ───────
        // Rule 3: Comparación de firma en tiempo constante
        if (!constantTimeEquals(computedHmac, receivedHmac)) {
            // No loguear el secret ni el HMAC recibido
            log.warn("Firma HMAC inválida en webhook MP — x-request-id: {}", xRequestId);
            throw new SignatureVerificationException("Firma HMAC inválida");
        }

        log.debug("Firma MP verificada correctamente — x-request-id: {}", xRequestId);
    }

    /**
     * Calcula HMAC-SHA256 sobre {@code data} usando {@code key} como secreto.
     *
     * @param key  Clave secreta (MP_WEBHOOK_SECRET)
     * @param data Cadena sobre la que calcular el HMAC (manifest)
     * @return Representación hexadecimal del HMAC calculado
     */
    private String computeHmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (NoSuchAlgorithmException e) {
            // HmacSHA256 está garantizado en el JDK — no debería ocurrir
            throw new IllegalStateException("HmacSHA256 no disponible en el JDK", e);
        } catch (InvalidKeyException e) {
            throw new SignatureVerificationException(
                    "Clave HMAC inválida — verifica MP_WEBHOOK_SECRET", e);
        }
    }

    /**
     * Comparación de strings en tiempo constante para evitar timing side-channels.
     * Equivalente a {@code MessageDigest.isEqual} pero sobre Strings.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        if (aBytes.length != bBytes.length) return false;

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= (aBytes[i] ^ bBytes[i]);
        }
        return result == 0;
    }
}
