package com.neogaming.review.dto;

import com.neogaming.common.enums.EstadoResena;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de moderación de una reseña.
 * Solo administradores pueden aprobar o rechazar reseñas.
 * El campo rejectReason es obligatorio cuando status = REJECTED.
 */
public record ReviewModerationRequest(

        @NotNull(message = "El estado de moderación es obligatorio")
        EstadoResena status,

        @Size(max = 500, message = "El motivo de rechazo no puede superar 500 caracteres")
        String rejectReason
) {}
