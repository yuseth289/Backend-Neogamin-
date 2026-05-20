-- =====================================================================
-- V9: Módulo de Checkout
-- =====================================================================
-- El checkout es la sesión de pago entre el carrito y la orden.
-- Tiene una duración limitada (30 min por defecto) y reserva stock.
--
-- Flujo:
--   1. Cliente inicia checkout → se reserva stock, se captura snapshot de precios
--   2. Cliente elige método de pago y dirección de entrega
--   3. Cliente paga → se crea la orden y se confirma el pago
--   4. Si el checkout expira o falla → se libera el stock reservado
--
-- Los precios y la dirección se capturan como JSONB para tener un
-- historial inmutable (si el precio del producto cambia, el checkout
-- ya tiene el precio al que el cliente acordó pagar).
-- =====================================================================

CREATE TABLE checkouts (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL,
    cart_id         UUID,           -- Referencia al carrito origen (para historial)

    -- Estado del checkout
    -- PENDING → COMPLETED (pago exitoso) | EXPIRED (timeout) | CANCELLED (usuario)
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',

    -- Snapshot de los ítems al momento de crear el checkout (JSONB inmutable)
    -- Formato: [{"productId":"uuid","name":"...","quantity":2,"unitPrice":350000}]
    items_snapshot  JSONB           NOT NULL,

    -- Dirección de entrega seleccionada (snapshot inmutable)
    -- Formato: {"street":"Cra 7","city":"Bogotá","department":"Cundinamarca",...}
    shipping_address JSONB,

    -- Totales calculados y fijos al momento del checkout
    subtotal        NUMERIC(14,2)   NOT NULL,
    shipping_cost   NUMERIC(14,2)   NOT NULL DEFAULT 0.00,
    total           NUMERIC(14,2)   NOT NULL,

    -- Método de pago seleccionado
    payment_method  VARCHAR(30),    -- MP_PSE | MP_NEQUI | MP_CREDIT_CARD | etc.

    -- Expiración del checkout (configurable, default 30 min)
    expires_at      TIMESTAMPTZ     NOT NULL,

    -- Auditoría
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_checkouts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_checkouts_user_id    ON checkouts (user_id);
CREATE INDEX idx_checkouts_status     ON checkouts (status);
CREATE INDEX idx_checkouts_expires_at ON checkouts (expires_at);

-- =====================================================================
-- Ítems del checkout (relación 1:N para poder hacer consultas SQL)
-- Estos datos también están en items_snapshot (JSONB) pero tener
-- la tabla relacional facilita queries de reporte y conciliación.
-- =====================================================================
CREATE TABLE checkout_items (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    checkout_id     UUID            NOT NULL,
    product_id      UUID            NOT NULL,
    seller_id       UUID            NOT NULL,   -- Desnormalizado para split de pago

    quantity        INTEGER         NOT NULL,
    unit_price      NUMERIC(14,2)   NOT NULL,   -- Precio con IVA al momento del checkout
    subtotal        NUMERIC(14,2)   NOT NULL,   -- unit_price × quantity

    CONSTRAINT fk_checkout_items_checkout
        FOREIGN KEY (checkout_id) REFERENCES checkouts (id) ON DELETE CASCADE
);

CREATE INDEX idx_checkout_items_checkout_id ON checkout_items (checkout_id);
