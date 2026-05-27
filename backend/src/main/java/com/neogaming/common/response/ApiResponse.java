package com.neogaming.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Envoltorio estándar para todas las respuestas de la API NeoGaming.
 *
 * Garantiza que todos los endpoints devuelvan el mismo formato JSON:
 * {
 *   "status":    "success" | "error",
 *   "message":   "Descripción opcional en español",
 *   "errorCode": "CODIGO_INTERNO" (solo en errores),
 *   "data":      { ... } | null,
 *   "timestamp": "2026-05-19T10:30:00Z"
 * }
 *
 * Uso típico en controladores:
 *   return ResponseEntity.ok(ApiResponse.ok(userResponse));
 *   return ResponseEntity.status(201).body(ApiResponse.created(nuevoProducto));
 *
 * Los campos null se omiten en la serialización JSON gracias a @JsonInclude.
 *
 * @param <T> Tipo del objeto retornado en el campo "data"
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String status,
        String message,
        String errorCode,
        T data,
        Instant timestamp
) {

    /** Respuesta exitosa con datos (sin mensaje adicional) */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("success", null, null, data, Instant.now());
    }

    /** Respuesta exitosa con datos y mensaje descriptivo (ej: "Perfil actualizado correctamente") */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>("success", message, null, data, Instant.now());
    }

    /** Respuesta de recurso creado (HTTP 201) con el nuevo objeto */
    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>("success", null, null, data, Instant.now());
    }

    /** Respuesta exitosa sin cuerpo de datos (ej: logout, eliminación) */
    public static ApiResponse<Void> noContent() {
        return new ApiResponse<>("success", null, null, null, Instant.now());
    }

    /** Respuesta de error con mensaje y código de error interno */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>("error", message, errorCode, null, Instant.now());
    }

    /** Respuesta de error con datos adicionales (ej: mapa de errores de validación por campo) */
    public static <T> ApiResponse<T> error(String message, String errorCode, T data) {
        return new ApiResponse<>("error", message, errorCode, data, Instant.now());
    }
}
