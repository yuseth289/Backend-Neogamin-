-- Permite quantity = 0 en inventory_movements para registrar ajustes que
-- establecen el stock a cero (marcar producto como agotado / conteo fisico = 0).
-- El constraint original (quantity > 0) bloqueaba estos movimientos validos.

ALTER TABLE inventory_movements
    DROP CONSTRAINT chk_movement_quantity,
    ADD CONSTRAINT chk_movement_quantity CHECK (quantity >= 0);
