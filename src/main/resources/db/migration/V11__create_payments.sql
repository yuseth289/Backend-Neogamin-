-- =====================================================================
-- V11: Módulo de Pagos
-- =====================================================================
-- Registro de pagos procesados por Mercado Pago Colombia.
--
-- Cada pago corresponde a un checkout. Cuando MP confirma el pago
-- (vía webhook), se crea o actualiza el registro y se genera la orden.
--
-- Métodos de pago soportados en Colombia:
--   MP_PSE          → Débito en línea (PSE — sistema bancario colombiano)
--   MP_NEQUI        → Billetera digital Nequi
--   MP_EFECTY       → Pago en efectivo en puntos Efecty
--   MP_CREDIT_CARD  → Tarjeta de crédito
--   MP_DEBIT_CARD   → Tarjeta débito
--   MP_ACCOUNT_MONEY → Saldo Mercado Pago
-- =====================================================================

CREATE TABLE payments (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    checkout_id     UUID            NOT NULL,
    user_id         UUID            NOT NULL,

    -- ID del pago asignado por Mercado Pago (para reconciliación)
    mp_payment_id   VARCHAR(100),

    -- ID de preferencia de MP (creado al iniciar el flujo de pago)
    mp_preference_id VARCHAR(100),

    -- Estado del pago en MP: pending | approved | rejected | cancelled | refunded
    mp_status       VARCHAR(30),

    -- Estado interno NeoGaming
    -- PENDING → APPROVED | REJECTED | CANCELLED | REFUNDED
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',

    -- Método de pago usado
    payment_method  VARCHAR(30)     NOT NULL,

    -- Monto pagado en COP
    amount          NUMERIC(14,2)   NOT NULL,

    -- Respuesta raw de MP para debugging (JSONB)
    mp_response     JSONB,

    -- Auditoría
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_payments_checkout UNIQUE (checkout_id),
    CONSTRAINT fk_payments_checkout FOREIGN KEY (checkout_id)
                                        REFERENCES checkouts (id),
    CONSTRAINT fk_payments_user     FOREIGN KEY (user_id)
                                        REFERENCES users (id)
);

CREATE INDEX idx_payments_mp_payment_id ON payments (mp_payment_id);
CREATE INDEX idx_payments_status        ON payments (status);
CREATE INDEX idx_payments_user_id       ON payments (user_id);
