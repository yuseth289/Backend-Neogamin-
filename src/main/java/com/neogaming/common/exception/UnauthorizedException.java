package com.neogaming.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando el usuario no está autenticado o sus credenciales son inválidas.
 * Retorna HTTP 401 Unauthorized.
 *
 * Ejemplos de uso:
 *  - Credenciales de login incorrectas (email o contraseña inválidos)
 *  - Token JWT expirado o revocado
 *  - Intento de acceso sin token en un endpoint protegido
 *  - Refresh token inválido o ya usado
 */
public class UnauthorizedException extends NeoGamingException {

    /**
     * @param message Descripción del problema de autenticación en español
     */
    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}
