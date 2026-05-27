-- =====================================================================
-- V13: Módulo de Reseñas
-- =====================================================================
-- Un comprador puede dejar una reseña por cada producto que haya comprado.
-- La restricción uq_reviews_user_product garantiza máximo una reseña por
-- usuario por producto.
--
-- Estados:
--   PENDING   → en moderación antes de publicarse
--   APPROVED  → visible en la página del producto
--   REJECTED  → rechazada por el moderador (inapropiada, spam, etc.)
-- =====================================================================

CREATE TABLE reviews (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID            NOT NULL,
    user_id     UUID            NOT NULL,
    order_id    UUID            NOT NULL,

    -- Calificación del 1 al 5
    rating      SMALLINT        NOT NULL CHECK (rating BETWEEN 1 AND 5),

    -- Contenido de la reseña (opcional — puede ser solo rating)
    title       VARCHAR(150),
    body        TEXT,

    -- Estado de moderación
    status      VARCHAR(20)     NOT NULL DEFAULT 'PENDING',

    -- Motivo de rechazo (solo se llena cuando status = REJECTED)
    reject_reason VARCHAR(500),

    -- Auditoría
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Un usuario solo puede reseñar el mismo producto una vez
    CONSTRAINT uq_reviews_user_product UNIQUE (user_id, product_id),

    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_reviews_user    FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT fk_reviews_order   FOREIGN KEY (order_id)   REFERENCES orders (id)
);

CREATE INDEX idx_reviews_product_id ON reviews (product_id);
CREATE INDEX idx_reviews_user_id    ON reviews (user_id);
CREATE INDEX idx_reviews_status     ON reviews (status);
