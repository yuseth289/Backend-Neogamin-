package com.neogaming.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando un recurso solicitado no existe en la base de datos.
 * Retorna HTTP 404 Not Found.
 *
 * Ejemplos de uso:
 *  - Usuario no encontrado por ID
 *  - Producto no encontrado por slug
 *  - Dirección no encontrada (o no pertenece al usuario)
 *  - Pedido no encontrado
 */
public class ResourceNotFoundException extends NeoGamingException {

    /**
     * @param resource Nombre legible del recurso (ej: "Usuario", "Producto", "Dirección")
     * @param id       Identificador con el que se intentó buscar (ej: UUID, slug)
     */
    public ResourceNotFoundException(String resource, String id) {
        super(resource + " no encontrado con id: " + id, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
