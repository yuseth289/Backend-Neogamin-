-- =====================================================================
-- V10: Módulo de Órdenes
-- =====================================================================
-- La orden es el registro permanente de una compra completada.
-- Se crea cuando el pago es confirmado por Mercado Pago.
--
-- Estructura:
--   order       → cabecera de la compra (usuario, totales, estado)
--   order_groups → agrupación por vendedor (para split de pago)
--   order_items  → ítems individuales con snapshot de precio
--
-- El campo items_snapshot y shipping_address guardan el estado inmutable
-- al momento de la compra — si el precio cambia después, la orden
-- siempre refleja lo que el cliente realmente pagó.
-- =====================================================================

CREATE TABLE orders (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL,
    checkout_id     UUID,           -- Referencia al checkout que generó esta orden

    -- Estado global de la orden
    -- PENDING_PAYMENT → PAID → PROCESSING → SHIPPED → DELIVERED | CANCELLED | REFUNDED
    status          VARCHAR(30)     NOT NULL DEFAULT 'PENDING_PAYMENT',

    -- Snapshot de la dirección de entrega (JSONB inmutable)
    shipping_address JSONB          NOT NULL,

    -- Totales en COP
    subtotal        NUMERIC(14,2)   NOT NULL,
    shipping_cost   NUMERIC(14,2)   NOT NULL DEFAULT 0.00,
    total           NUMERIC(14,2)   NOT NULL,

    -- Referencia al pago confirmado (se llena cuando MP confirma)
    payment_id      UUID,

    -- Auditoría
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_orders_user_id    ON orders (user_id);
CREATE INDEX idx_orders_status     ON orders (status);
CREATE INDEX idx_orders_payment_id ON orders (payment_id);

-- =====================================================================
-- Grupos de la orden por vendedor
-- =====================================================================
-- Cada orden puede contener productos de múltiples vendedores.
-- Cada grupo representa los ítems de un vendedor específico y tiene
-- su propio estado de preparación/envío.
-- =====================================================================
CREATE TABLE order_groups (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID        NOT NULL,
    seller_id   UUID        NOT NULL,

    -- Estado del grupo para este vendedor específico
    -- PENDING → PROCESSING → SHIPPED → DELIVERED
    status      VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    -- Subtotal del grupo (suma de ítems de este vendedor)
    subtotal    NUMERIC(14,2) NOT NULL,

    -- Número de seguimiento del envío (lo agrega el vendedor)
    tracking_number VARCHAR(100),

    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_order_groups_order  FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_groups_seller FOREIGN KEY (seller_id) REFERENCES sellers (id)
);

CREATE INDEX idx_order_groups_order_id  ON order_groups (order_id);
CREATE INDEX idx_order_groups_seller_id ON order_groups (seller_id);

-- =====================================================================
-- Ítems individuales de la orden
-- =====================================================================
CREATE TABLE order_items (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID            NOT NULL,
    order_group_id  UUID            NOT NULL,
    product_id      UUID            NOT NULL,

    -- Snapshot de datos del producto al momento de la compra
    product_name    VARCHAR(200)    NOT NULL,
    product_sku     VARCHAR(100),

    quantity        INTEGER         NOT NULL,
    unit_price      NUMERIC(14,2)   NOT NULL,   -- Precio con IVA pagado
    subtotal        NUMERIC(14,2)   NOT NULL,

    CONSTRAINT fk_order_items_order       FOREIGN KEY (order_id)       REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_group       FOREIGN KEY (order_group_id) REFERENCES order_groups (id) ON DELETE CASCADE
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
