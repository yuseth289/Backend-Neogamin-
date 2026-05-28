-- ============================================================
-- V27: Agrega columna created_at a checkout_items
-- La entidad CheckoutItem la requiere pero la migracion V9
-- no la incluyo originalmente.
-- ============================================================

ALTER TABLE checkout_items
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
