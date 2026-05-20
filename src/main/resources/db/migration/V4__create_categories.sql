-- =====================================================================
-- V4: Módulo de Categorías
-- =====================================================================
-- Tabla de categorías de productos con soporte para jerarquía de dos niveles:
-- categoría padre → subcategoría hijo.
--
-- Ejemplo de jerarquía:
--   "Periféricos" (padre, parent_id = NULL)
--     └── "Teclados" (hijo, parent_id = UUID de Periféricos)
--     └── "Audífonos" (hijo)
--   "Consolas" (padre)
--     └── "PlayStation" (hijo)
-- =====================================================================

CREATE TABLE categories (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Nombre de la categoría y slug URL-amigable
    name            VARCHAR(100)    NOT NULL,
    slug            VARCHAR(120)    NOT NULL,

    /** Descripción opcional de la categoría */
    description     TEXT,

    /** URL de imagen representativa de la categoría */
    image_url       VARCHAR(500),

    -- Referencia al padre para crear la jerarquía de dos niveles.
    -- NULL significa que es una categoría raíz (padre).
    parent_id       UUID,

    -- Las categorías inactivas no aparecen en el catálogo público
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',

    -- Auditoría
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Restricciones
    CONSTRAINT uq_categories_slug   UNIQUE (slug),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id)
                                        REFERENCES categories (id) ON DELETE SET NULL
);

-- Índice para obtener subcategorías de un padre rápidamente
CREATE INDEX idx_categories_parent_id ON categories (parent_id);
CREATE INDEX idx_categories_status    ON categories (status);
