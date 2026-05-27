package com.neogaming.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando el usuario está autenticado pero no tiene permisos para realizar la acción.
 * Retorna HTTP 403 Forbidden.
 *
 * Diferencia con UnauthorizedException:
 *  - 401 Unauthorized = "No sé quién eres" (sin autenticar)
 *  - 403 Forbidden    = "Sé quién eres pero no puedes hacer esto" (sin autorización)
 *
 * Ejemplos de uso:
 *  - Un CLIENT intenta acceder a endpoints de ADMIN
 *  - Un SELLER intenta modificar el producto de otro SELLER
 *  - Un usuario intenta ver el pedido de otro usuario
 */
public class ForbiddenException extends NeoGamingException {

    /**
     * @param message Descripción del acceso denegado en español
     */
    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }
}
