-- =====================================================================
-- V15: Vistas y funciones auxiliares para Analytics
-- =====================================================================
-- Las vistas materializan cálculos costosos que se usan en los
-- dashboards de vendedores y administradores.
--
-- Nota: estas son vistas simples (no MATERIALIZED) para compatibilidad
-- con transacciones Flyway. Si el volumen de datos crece, se puede
-- convertir a MATERIALIZED VIEW con REFRESH CONCURRENTLY.
-- =====================================================================

-- Vista: ventas por vendedor (órdenes entregadas o en proceso)
CREATE VIEW v_seller_sales AS
SELECT
    og.seller_id,
    DATE_TRUNC('month', o.created_at) AS mes,
    COUNT(DISTINCT og.order_id)       AS total_ordenes,
    SUM(oi.subtotal)                  AS ingresos_brutos,
    COUNT(DISTINCT oi.product_id)     AS productos_distintos
FROM order_groups og
JOIN orders o      ON o.id = og.order_id
JOIN order_items oi ON oi.order_group_id = og.id
WHERE og.status NOT IN ('CANCELLED')
  AND o.status  NOT IN ('CANCELLED', 'REFUNDED')
GROUP BY og.seller_id, DATE_TRUNC('month', o.created_at);

-- Vista: productos más vendidos (por cantidad de unidades vendidas)
CREATE VIEW v_top_products AS
SELECT
    oi.product_id,
    oi.product_name,
    SUM(oi.quantity)   AS unidades_vendidas,
    SUM(oi.subtotal)   AS ingresos_totales,
    COUNT(DISTINCT oi.order_id) AS ordenes
FROM order_items oi
JOIN orders o ON o.id = oi.order_id
WHERE o.status NOT IN ('CANCELLED', 'REFUNDED')
GROUP BY oi.product_id, oi.product_name;

-- Vista: resumen de pagos (para panel de administrador)
CREATE VIEW v_payment_summary AS
SELECT
    DATE_TRUNC('day', p.created_at) AS dia,
    p.status,
    p.payment_method,
    COUNT(*)           AS total_transacciones,
    SUM(p.amount)      AS monto_total
FROM payments p
GROUP BY DATE_TRUNC('day', p.created_at), p.status, p.payment_method;
