-- =====================================================================
-- V3: Módulo de Vendedores (Sellers)
-- =====================================================================
-- Crea las tablas para perfiles de vendedor y sus cuentas bancarias
-- para recibir pagos mediante split de Mercado Pago Colombia.
--
-- Un usuario con rol SELLER tiene exactamente un perfil en esta tabla.
-- El vendedor debe ser aprobado por un ADMIN antes de poder publicar.
-- =====================================================================

-- Perfil del vendedor con datos fiscales colombianos
CREATE TABLE sellers (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL,

    -- Datos de la tienda
    store_name          VARCHAR(100)    NOT NULL,
    store_slug          VARCHAR(120)    NOT NULL,       -- Generado desde store_name, único
    store_description   TEXT,
    store_logo_url      VARCHAR(500),
    store_banner_url    VARCHAR(500),

    -- Datos fiscales colombianos
    tipo_documento      VARCHAR(20)     NOT NULL,       -- CC, NIT, CE, PASSPORT, TI
    numero_documento    VARCHAR(30)     NOT NULL,
    razon_social        VARCHAR(200),                   -- Razón social para NIT
    tipo_regimen        VARCHAR(30)     NOT NULL,       -- RESPONSABLE_IVA | NO_RESPONSABLE_IVA

    -- Datos de contacto del vendedor
    phone               VARCHAR(20),
    address             VARCHAR(300),
    city                VARCHAR(100),
    department          VARCHAR(100),

    -- Integración con Mercado Pago Colombia
    -- El access_token se obtiene tras el flujo OAuth de MP
    mp_access_token     VARCHAR(500),                   -- Token de MP (cifrado en producción)
    mp_user_id          VARCHAR(50),                    -- ID de usuario en Mercado Pago

    -- Estado del vendedor en la plataforma
    -- PENDING → admin lo revisa → ACTIVE | SUSPENDED
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',

    -- Auditoría
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Restricciones
    CONSTRAINT uq_sellers_user_id      UNIQUE (user_id),
    CONSTRAINT uq_sellers_store_slug   UNIQUE (store_slug),
    CONSTRAINT fk_sellers_user         FOREIGN KEY (user_id)
                                           REFERENCES users (id) ON DELETE CASCADE
);

-- Índices para búsquedas frecuentes
CREATE INDEX idx_sellers_user_id ON sellers (user_id);
CREATE INDEX idx_sellers_status  ON sellers (status);
CREATE INDEX idx_sellers_slug    ON sellers (store_slug);

-- =====================================================================
-- Cuentas bancarias del vendedor para recibir pagos
-- Un vendedor puede tener múltiples cuentas pero solo una activa a la vez
-- =====================================================================
CREATE TABLE seller_payment_accounts (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id       UUID            NOT NULL,

    -- Datos bancarios colombianos
    bank_name       VARCHAR(100)    NOT NULL,   -- Ej: "Bancolombia", "Davivienda"
    account_type    VARCHAR(20)     NOT NULL,   -- AHORROS | CORRIENTE
    account_number  VARCHAR(30)     NOT NULL,   -- Número de cuenta (cifrado en producción)
    account_holder  VARCHAR(200)    NOT NULL,   -- Nombre del titular
    document_type   VARCHAR(20)     NOT NULL,   -- CC | NIT | CE
    document_number VARCHAR(30)     NOT NULL,   -- Cédula o NIT del titular

    -- Solo una cuenta puede ser la activa para recibir pagos
    is_active       BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Auditoría
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Restricciones
    CONSTRAINT fk_payment_accounts_seller
        FOREIGN KEY (seller_id) REFERENCES sellers (id) ON DELETE CASCADE
);

-- Índice para buscar cuentas por vendedor
CREATE INDEX idx_payment_accounts_seller_id ON seller_payment_accounts (seller_id);
