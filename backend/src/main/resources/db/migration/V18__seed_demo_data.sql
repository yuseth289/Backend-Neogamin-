-- ============================================================
-- V18: Permite reseñas sin orden (order_id opcional)
-- Los datos de demo fueron removidos para producción.
-- ============================================================

ALTER TABLE reviews ALTER COLUMN order_id DROP NOT NULL;
