package com.neogaming.notification.dto;

import java.util.Map;

/**
 * Contexto para renderizar y enviar un correo electrónico.
 *
 * @param to           Dirección de destino
 * @param subject      Asunto del correo
 * @param templateName Nombre de la plantilla Thymeleaf en templates/email/ (sin extensión)
 * @param variables    Variables inyectadas en la plantilla
 */
public record EmailContext(
        String to,
        String subject,
        String templateName,
        Map<String, Object> variables
) {}
