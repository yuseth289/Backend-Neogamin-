-- =====================================================================
-- V8: Módulo de Carrito de Compras
-- =====================================================================
-- Tabla del carrito y sus ítems.
--
-- Un usuario tiene exactamente un carrito activo a la vez (ACTIVE).
-- Los ítems del carrito no reservan stock — la reserva ocurre al
-- crear el checkout. Esto evita bloquear stock indefinidamente.
--
-- Precio en el ítem: se almacena el precio al momento de agregar al carrito
-- para detectar si el precio cambió antes del checkout.
-- =====================================================================

CREATE TABLE carts (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | CONVERTED | ABANDONED
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_carts_user_active UNIQUE (user_id, status),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_carts_user_id ON carts (user_id);

CREATE TABLE cart_items (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id         UUID            NOT NULL,
    product_id      UUID            NOT NULL,

    quantity        INTEGER         NOT NULL,

    -- Precio unitario final (con IVA) al momento de agregar al carrito
    -- Se guarda para detectar cambios de precio antes del checkout
    unit_price      NUMERIC(14,2)   NOT NULL,

    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_cart_item         UNIQUE (cart_id, product_id),
    CONSTRAINT chk_cart_item_qty    CHECK (quantity > 0),
    CONSTRAINT fk_cart_items_cart   FOREIGN KEY (cart_id)   REFERENCES carts (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE INDEX idx_cart_items_cart_id ON cart_items (cart_id);
