package com.neogaming.common.exception;

import com.neogaming.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para toda la API.
 *
 * Intercepta todas las excepciones lanzadas en cualquier controlador y las convierte
 * en respuestas HTTP con el formato estándar de NeoGaming (ApiResponse).
 *
 * Jerarquía de manejo (de más específico a más general):
 *  1. NeoGamingException          → errores de negocio propios del sistema
 *  2. MethodArgumentNotValidException → errores de validación de campos (@Valid)
 *  3. AccessDeniedException        → acceso denegado por Spring Security
 *  4. BadCredentialsException      → credenciales incorrectas en login
 *  5. Exception                    → cualquier error no controlado (500)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Maneja errores del microservicio Python AI (AIServiceException → HTTP 502).
     */
    @ExceptionHandler(AIServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleAIServiceException(AIServiceException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }

    /**
     * Maneja todas las excepciones propias del sistema (NeoGamingException y subclases).
     * El HttpStatus y el errorCode los define cada excepción específica.
     */
    @ExceptionHandler(NeoGamingException.class)
    public ResponseEntity<ApiResponse<Void>> handleNeoGamingException(NeoGamingException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }

    /**
     * Maneja errores de validación de Jakarta (@Valid / @NotBlank / @Size, etc.).
     * Retorna HTTP 400 con un mapa de campo → mensaje de error.
     * Ejemplo: { "email": "El email es obligatorio", "password": "Mínimo 8 caracteres" }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {

        // Recopila todos los errores de campo en un mapa legible
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Valor inválido",
                        (existing, replacement) -> existing // en caso de campos duplicados, conserva el primero
                ));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Errores de validación en los campos enviados", "VALIDATION_ERROR", errors));
    }

    /**
     * Maneja accesos denegados lanzados por Spring Security (@PreAuthorize, etc.).
     * Retorna HTTP 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tienes permisos para realizar esta acción", "FORBIDDEN"));
    }

    /**
     * Maneja credenciales inválidas durante el proceso de autenticación.
     * Retorna HTTP 401.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Credenciales inválidas", "UNAUTHORIZED"));
    }

    /**
     * Captura cualquier excepción no controlada por los handlers anteriores.
     * Retorna HTTP 500 con un mensaje genérico (sin exponer detalles internos al cliente).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Ha ocurrido un error interno. Por favor intenta de nuevo.", "INTERNAL_ERROR"));
    }
}
