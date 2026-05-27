package com.neogaming.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando existe un conflicto con el estado actual del recurso.
 * Retorna HTTP 409 Conflict.
 *
 * Ejemplos de uso:
 *  - Intento de registrar un email que ya está en uso
 *  - Intento de agregar un producto que ya está en la wishlist
 *  - Intento de crear un vendedor cuando el usuario ya tiene uno
 *  - Stock insuficiente al reservar (el recurso está en conflicto con la cantidad pedida)
 */
public class ConflictException extends NeoGamingException {

    /**
     * @param message   Descripción del conflicto en español
     * @param errorCode Código interno (ej: "EMAIL_YA_EXISTE", "STOCK_INSUFICIENTE")
     */
    public ConflictException(String message, String errorCode) {
        super(message, HttpStatus.CONFLICT, errorCode);
    }
}
