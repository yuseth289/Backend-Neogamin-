-- =====================================================================
-- V14: Módulo de Wishlist (Lista de Deseos)
-- =====================================================================
-- Permite a los compradores guardar productos de interés para comprarlos
-- después. Un usuario puede tener múltiples wishlists (pública o privada).
--
-- Estructura:
--   wishlists      → listas de deseos del usuario
--   wishlist_items → productos guardados en cada lista
-- =====================================================================

CREATE TABLE wishlists (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID            NOT NULL,
    name        VARCHAR(100)    NOT NULL,
    -- true = visible para otros usuarios mediante enlace
    is_public   BOOLEAN         NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_wishlists_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE wishlist_items (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    wishlist_id     UUID        NOT NULL,
    product_id      UUID        NOT NULL,
    added_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Un producto solo puede aparecer una vez por lista
    CONSTRAINT uq_wishlist_items UNIQUE (wishlist_id, product_id),

    CONSTRAINT fk_wishlist_items_wishlist FOREIGN KEY (wishlist_id)
                                              REFERENCES wishlists (id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_items_product  FOREIGN KEY (product_id)
                                              REFERENCES products (id)
);

CREATE INDEX idx_wishlists_user_id       ON wishlists (user_id);
CREATE INDEX idx_wishlist_items_wishlist ON wishlist_items (wishlist_id);
CREATE INDEX idx_wishlist_items_product  ON wishlist_items (product_id);
