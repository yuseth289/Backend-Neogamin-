package com.neogaming.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción base del sistema NeoGaming.
 *
 * Todas las excepciones de negocio deben extender esta clase para mantener
 * un formato de error consistente en toda la API.
 *
 * Estructura de respuesta al cliente:
 * {
 *   "status": "error",
 *   "message": "Mensaje legible en español",
 *   "errorCode": "CODIGO_INTERNO",
 *   "timestamp": "..."
 * }
 */
public class NeoGamingException extends RuntimeException {

    /** Código HTTP que se enviará en la respuesta (ej: 404, 409, 422) */
    private final HttpStatus status;

    /** Código interno legible por el frontend para manejar el error programáticamente */
    private final String errorCode;

    /**
     * @param message   Mensaje descriptivo en español para el usuario final
     * @param status    Código HTTP de la respuesta
     * @param errorCode Código de error interno (ej: "EMAIL_YA_EXISTE", "STOCK_INSUFICIENTE")
     */
    public NeoGamingException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
