-- ─────────────────────────────────────────────
-- TABLA: addresses
-- ─────────────────────────────────────────────
CREATE TABLE addresses (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    label       VARCHAR(50)  NOT NULL,
    street      VARCHAR(200) NOT NULL,
    number      VARCHAR(20)  NOT NULL,
    floor       VARCHAR(10),
    apartment   VARCHAR(10),
    city        VARCHAR(100) NOT NULL,
    department  VARCHAR(100) NOT NULL,
    country     VARCHAR(100) NOT NULL DEFAULT 'Colombia',
    postal_code VARCHAR(20),
    is_primary  BOOLEAN      NOT NULL DEFAULT FALSE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_addresses_user_id ON addresses (user_id);
