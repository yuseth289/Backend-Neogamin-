-- =====================================================================
-- V12: Módulo de Facturas
-- =====================================================================
-- Genera facturas electrónicas por cada pago aprobado.
-- Una factura corresponde a una orden completa (todos sus grupos).
--
-- Estados:
--   DRAFT      → recién generada, pendiente de envío
--   ISSUED     → enviada al comprador por correo
--   CANCELLED  → anulada (por devolución o error)
-- =====================================================================

CREATE TABLE invoices (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID            NOT NULL,
    payment_id      UUID            NOT NULL,
    user_id         UUID            NOT NULL,

    -- Número secuencial legible (ej: NG-2026-000001)
    invoice_number  VARCHAR(30)     NOT NULL UNIQUE,

    -- Estado de la factura
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',

    -- Datos del comprador en el momento de la compra (snapshot)
    buyer_name      VARCHAR(200)    NOT NULL,
    buyer_email     VARCHAR(200)    NOT NULL,
    buyer_document  VARCHAR(20),

    -- Totales
    subtotal        NUMERIC(14,2)   NOT NULL,
    tax_amount      NUMERIC(14,2)   NOT NULL DEFAULT 0,
    total           NUMERIC(14,2)   NOT NULL,

    -- Ítems de la factura en formato JSONB (snapshot)
    items           JSONB           NOT NULL DEFAULT '[]',

    -- Fecha de emisión y anulación
    issued_at       TIMESTAMPTZ,
    cancelled_at    TIMESTAMPTZ,
    cancel_reason   VARCHAR(500),

    -- Auditoría
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_invoices_order   UNIQUE (order_id),
    CONSTRAINT fk_invoices_order   FOREIGN KEY (order_id)   REFERENCES orders (id),
    CONSTRAINT fk_invoices_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_invoices_user    FOREIGN KEY (user_id)    REFERENCES users (id)
);

-- Secuencia para generar invoice_number consecutivo por año
CREATE SEQUENCE invoice_seq START 1 INCREMENT 1;

CREATE INDEX idx_invoices_user_id   ON invoices (user_id);
CREATE INDEX idx_invoices_status    ON invoices (status);
CREATE INDEX idx_invoices_issued_at ON invoices (issued_at);
