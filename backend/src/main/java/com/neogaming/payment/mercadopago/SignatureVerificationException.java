package com.neogaming.payment.mercadopago;

/**
 * Excepción lanzada cuando la verificación de firma HMAC-SHA256
 * del webhook de Mercado Pago falla.
 *
 * Causas posibles:
 *  - Header x-signature ausente o malformado
 *  - Timestamp fuera de la ventana de 300 segundos (replay)
 *  - HMAC calculado no coincide con el recibido (tampering o secret incorrecto)
 */
public class SignatureVerificationException extends RuntimeException {

    public SignatureVerificationException(String message) {
        super(message);
    }

    public SignatureVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
