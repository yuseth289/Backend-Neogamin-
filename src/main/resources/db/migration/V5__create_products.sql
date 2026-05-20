-- =====================================================================
-- V5: Módulo de Productos e Imágenes
-- =====================================================================
-- Tabla principal de productos del catálogo y sus imágenes asociadas.
--
-- Modelo de precios con IVA colombiano:
--   base_price  = precio antes de IVA
--   iva_percent = porcentaje de IVA (generalmente 19%)
--   final_price = base_price * (1 + iva_percent/100) — calculado en app
--
-- Estados del producto:
--   DRAFT  → el vendedor lo está configurando, no visible públicamente
--   ACTIVE → publicado y visible en el catálogo
--   PAUSED → vendedor lo pausa temporalmente (no es DRAFT ni borrado)
--   DELETED → soft delete
-- =====================================================================

CREATE TABLE products (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Relaciones
    seller_id       UUID            NOT NULL,   -- Vendedor propietario
    category_id     UUID,                       -- Categoría (puede quedar NULL si se desactiva)

    -- Datos del producto
    name            VARCHAR(200)    NOT NULL,
    slug            VARCHAR(250)    NOT NULL,   -- Para URLs: /products/{slug}
    description     TEXT,
    brand           VARCHAR(100),
    sku             VARCHAR(100),               -- Código interno del vendedor

    -- Precios colombianos (en COP)
    -- Se almacenan como NUMERIC(14,2) para precisión monetaria
    base_price      NUMERIC(14,2)   NOT NULL,   -- Precio sin IVA
    iva_percent     NUMERIC(5,2)    NOT NULL DEFAULT 19.00, -- % de IVA (19% por defecto)

    -- Estado y visibilidad
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',

    -- Auditoría
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Restricciones
    CONSTRAINT uq_products_slug     UNIQUE (slug),
    CONSTRAINT chk_base_price       CHECK (base_price >= 0),
    CONSTRAINT chk_iva_percent      CHECK (iva_percent >= 0 AND iva_percent <= 100),
    CONSTRAINT fk_products_seller   FOREIGN KEY (seller_id)
                                        REFERENCES sellers (id) ON DELETE CASCADE,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id)
                                        REFERENCES categories (id) ON DELETE SET NULL
);

-- Índices para búsquedas y filtros del catálogo
CREATE INDEX idx_products_seller_id   ON products (seller_id);
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_status      ON products (status);
CREATE INDEX idx_products_slug        ON products (slug);

-- =====================================================================
-- Imágenes de productos
-- =====================================================================
-- Un producto puede tener múltiples imágenes. Solo una puede ser la
-- principal (is_primary = true). Las demás son imágenes adicionales
-- del producto mostradas en la galería.
-- =====================================================================
CREATE TABLE product_images (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID        NOT NULL,

    url         VARCHAR(500) NOT NULL,  -- URL en CDN/S3
    alt_text    VARCHAR(200),           -- Texto alternativo para accesibilidad
    sort_order  INTEGER     NOT NULL DEFAULT 0, -- Orden en la galería
    is_primary  BOOLEAN     NOT NULL DEFAULT FALSE,

    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_product_images_product
        FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE INDEX idx_product_images_product_id ON product_images (product_id);
