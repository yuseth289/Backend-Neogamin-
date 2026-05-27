-- =====================================================================
-- V7: Módulo de Ofertas y Descuentos
-- =====================================================================
-- Tabla de ofertas aplicadas sobre productos.
--
-- Tipos de descuento (TipoDescuento enum):
--   PERCENTAGE → porcentaje sobre el precio final (ej: 20% OFF)
--   FIXED      → monto fijo en COP (ej: $50.000 de descuento)
--
-- Una oferta tiene vigencia definida por start_date y end_date.
-- Solo puede haber UNA oferta activa por producto a la vez.
-- =====================================================================

CREATE TABLE offers (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      UUID            NOT NULL,

    -- Nombre descriptivo de la oferta (ej: "Black Friday 2026")
    name            VARCHAR(100)    NOT NULL,

    -- Tipo de descuento y valor
    discount_type   VARCHAR(20)     NOT NULL,   -- PERCENTAGE | FIXED
    discount_value  NUMERIC(14,2)   NOT NULL,   -- % o monto en COP

    -- Precio calculado con descuento (para facilitar queries de catálogo)
    -- Se persiste porque la consulta de catálogo lo necesita frecuentemente
    discounted_price NUMERIC(14,2)  NOT NULL,

    -- Vigencia de la oferta
    start_date      TIMESTAMPTZ     NOT NULL,
    end_date        TIMESTAMPTZ     NOT NULL,

    -- Estado de la oferta (ACTIVE | INACTIVE)
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',

    -- Auditoría
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Restricciones
    CONSTRAINT chk_discount_value   CHECK (discount_value > 0),
    CONSTRAINT chk_offer_dates      CHECK (end_date > start_date),
    CONSTRAINT fk_offers_product    FOREIGN KEY (product_id)
                                        REFERENCES products (id) ON DELETE CASCADE
);

CREATE INDEX idx_offers_product_id ON offers (product_id);
CREATE INDEX idx_offers_status     ON offers (status);
CREATE INDEX idx_offers_dates      ON offers (start_date, end_date);
