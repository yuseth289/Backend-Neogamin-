-- ============================================================
-- V28: Corrige el unique constraint de carts
-- El constraint original UNIQUE(user_id, status) bloqueaba
-- hacer mas de una compra por usuario ya que no puede haber
-- dos carts con status CONVERTED para el mismo usuario.
-- Se reemplaza por un indice parcial que solo aplica al status ACTIVE.
-- ============================================================

ALTER TABLE carts DROP CONSTRAINT uq_carts_user_active;

CREATE UNIQUE INDEX uq_carts_user_active
    ON carts (user_id)
    WHERE status = 'ACTIVE';
