-- V22: Tablas para deduplicación de webhooks e idempotencia de pagos
-- PagoKit integration — Mercado Pago Colombia
-- Generado el 2026-05-24

-- ─── webhook_events_processed ──────────────────────────────────────────────────
-- Almacena el x-request-id de cada webhook ya procesado para evitar
-- procesar el mismo evento más de una vez (replay protection).
CREATE TABLE IF NOT EXISTS webhook_events_processed (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    provider      VARCHAR(50)  NOT NULL,
    event_id      VARCHAR(255) NOT NULL,   -- x-request-id de Mercado Pago
    processed_at  TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT pk_webhook_events_processed PRIMARY KEY (id),
    CONSTRAINT uq_webhook_events_provider_event UNIQUE (provider, event_id)
);

-- Índice compuesto para búsqueda rápida por (provider, event_id) en cada webhook
CREATE INDEX IF NOT EXISTS idx_webhook_events_provider_event
    ON webhook_events_processed (provider, event_id);

-- ─── idempotency_keys ──────────────────────────────────────────────────────────
-- Registra los UUID usados como X-Idempotency-Key en cada llamada saliente
-- a la API de Mercado Pago, para evitar crear preferencias o pagos duplicados.
CREATE TABLE IF NOT EXISTS idempotency_keys (
    id         UUID         NOT NULL DEFAULT gen_random_uuid(),
    key        VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT pk_idempotency_keys PRIMARY KEY (id),
    CONSTRAINT uq_idempotency_keys_key UNIQUE (key)
);

-- Índice para búsqueda por clave (usado antes de cada POST a MP)
CREATE INDEX IF NOT EXISTS idx_idempotency_keys_key
    ON idempotency_keys (key);
