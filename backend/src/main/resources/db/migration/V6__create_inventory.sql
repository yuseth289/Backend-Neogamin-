-- =====================================================================
-- V6: Módulo de Inventario
-- =====================================================================
-- Gestión del stock de productos con modelo de stock físico vs reservado.
--
-- Modelo de stock:
--   physical_stock  = unidades físicamente disponibles (en bodega)
--   reserved_stock  = unidades reservadas por checkouts pendientes
--   available_stock = physical_stock - reserved_stock (calculado en app)
--
-- El available_stock NO se persiste para siempre reflejar el stock real.
--
-- Movimientos de inventario:
--   Cada cambio en el stock se registra en inventory_movements para
--   tener un historial completo (auditoría, disputas, conciliación).
-- =====================================================================

CREATE TABLE inventory (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      UUID            NOT NULL,

    -- Stock físico: unidades reales en bodega del vendedor
    physical_stock  INTEGER         NOT NULL DEFAULT 0,

    -- Stock reservado: unidades bloqueadas por checkouts pendientes de pago.
    -- Se libera cuando el checkout expira o el pago falla.
    -- Se confirma cuando el pago es exitoso → pasa a reducir physical_stock.
    reserved_stock  INTEGER         NOT NULL DEFAULT 0,

    -- Auditoría
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Restricciones
    CONSTRAINT uq_inventory_product UNIQUE (product_id),
    CONSTRAINT chk_physical_stock   CHECK (physical_stock >= 0),
    CONSTRAINT chk_reserved_stock   CHECK (reserved_stock >= 0),
    CONSTRAINT chk_stock_coherencia CHECK (reserved_stock <= physical_stock),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id)
                                        REFERENCES products (id) ON DELETE CASCADE
);

-- =====================================================================
-- Historial de movimientos de inventario
-- =====================================================================
-- Registra cada cambio en el stock para auditoría y conciliación.
-- Tipos de movimiento: IN (entrada), OUT (salida confirmada),
-- RESERVE (reserva), RELEASE (liberación de reserva), ADJUST (ajuste manual)
-- =====================================================================
CREATE TABLE inventory_movements (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_id    UUID            NOT NULL,
    product_id      UUID            NOT NULL,

    -- Tipo de movimiento (mapea a TipoMovimientoStock enum)
    movement_type   VARCHAR(20)     NOT NULL,

    -- Cantidad del movimiento (siempre positiva — el tipo indica la dirección)
    quantity        INTEGER         NOT NULL,

    -- Stock resultante después del movimiento (para fácil auditoría)
    physical_after  INTEGER         NOT NULL,
    reserved_after  INTEGER         NOT NULL,

    -- Referencia al documento que causó el movimiento (checkout, orden, etc.)
    reference_id    UUID,           -- UUID del checkout u orden relacionado
    notes           VARCHAR(300),   -- Descripción del motivo del ajuste

    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_movement_quantity CHECK (quantity > 0),
    CONSTRAINT fk_movements_inventory FOREIGN KEY (inventory_id)
                                          REFERENCES inventory (id) ON DELETE CASCADE
);

CREATE INDEX idx_inventory_movements_inventory_id ON inventory_movements (inventory_id);
CREATE INDEX idx_inventory_movements_product_id   ON inventory_movements (product_id);
