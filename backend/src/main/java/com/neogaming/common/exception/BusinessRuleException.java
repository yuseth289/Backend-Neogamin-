package com.neogaming.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando una operación viola una regla de negocio.
 * Retorna HTTP 422 Unprocessable Entity.
 *
 * Ejemplos de uso:
 *  - El carrito está vacío al intentar iniciar checkout
 *  - El vendedor no tiene estado ACTIVE
 *  - Las contraseñas de confirmación no coinciden
 *  - La cuenta del usuario está suspendida
 */
public class BusinessRuleException extends NeoGamingException {

    public BusinessRuleException(String message, String errorCode) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, errorCode);
    }
}
