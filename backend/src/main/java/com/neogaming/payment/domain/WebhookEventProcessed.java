package com.neogaming.payment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Registra cada webhook de Mercado Pago ya procesado.
 *
 * Se usa para deduplicación: antes de procesar cualquier notificación,
 * se verifica que el {@code eventId} (x-request-id de MP) no exista en
 * esta tabla. Si ya existe, el evento se descarta silenciosamente.
 *
 * // Rule 3: Replay protection a nivel de base de datos.
 */
@Entity
@Table(
    name = "webhook_events_processed",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_webhook_events_provider_event",
        columnNames = {"provider", "event_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEventProcessed {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Proveedor de pagos (siempre "mercadopago" para este módulo) */
    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    /**
     * ID único del evento — corresponde al header {@code x-request-id}
     * enviado por Mercado Pago en cada notificación webhook.
     */
    @Column(name = "event_id", nullable = false, length = 255)
    private String eventId;

    /** Momento en que se procesó el evento */
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
