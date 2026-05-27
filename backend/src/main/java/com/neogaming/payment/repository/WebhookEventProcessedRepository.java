package com.neogaming.payment.repository;

import com.neogaming.payment.domain.WebhookEventProcessed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositorio para la tabla de deduplicación de webhooks.
 *
 * Antes de procesar cualquier evento de MP, el controller consulta
 * {@link #existsByProviderAndEventId} para descartar duplicados.
 *
 * // Rule 3: Replay protection — deduplicar por (provider, event_id).
 */
public interface WebhookEventProcessedRepository extends JpaRepository<WebhookEventProcessed, UUID> {

    /**
     * Verifica si ya se procesó un evento con ese ID para el proveedor indicado.
     *
     * @param provider Nombre del proveedor (ej: "mercadopago")
     * @param eventId  Identificador único del evento (x-request-id de MP)
     * @return {@code true} si el evento ya fue procesado
     */
    boolean existsByProviderAndEventId(String provider, String eventId);
}
