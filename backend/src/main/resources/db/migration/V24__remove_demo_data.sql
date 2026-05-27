-- ============================================================
-- V24: ELIMINAR DATOS DE DEMO (insertados por V18)
-- Los 50 productos de demo tienen UUIDs con prefijos fijos:
--   a1000001-* (TechStore Colombia)
--   a2000002-* (PC Master Race CO)
--   a3000003-* (Gamer Paradise)
--   a4000004-* (NextGen Gaming)
--   a5000005-* (Perifericos Pro)
-- Esta migración es idempotente: si no existen, los DELETE son no-op.
-- ============================================================

DO $$
BEGIN

  -- Eliminar en orden de dependencias FK

  DELETE FROM reviews
  WHERE product_id::text SIMILAR TO '(a1000001|a2000002|a3000003|a4000004|a5000005)%';

  DELETE FROM offers
  WHERE product_id::text SIMILAR TO '(a1000001|a2000002|a3000003|a4000004|a5000005)%';

  DELETE FROM wishlist_items
  WHERE product_id::text SIMILAR TO '(a1000001|a2000002|a3000003|a4000004|a5000005)%';

  DELETE FROM cart_items
  WHERE product_id::text SIMILAR TO '(a1000001|a2000002|a3000003|a4000004|a5000005)%';

  DELETE FROM checkout_items
  WHERE product_id::text SIMILAR TO '(a1000001|a2000002|a3000003|a4000004|a5000005)%';

  DELETE FROM inventory_movements
  WHERE inventory_id IN (
    SELECT id FROM inventory
    WHERE product_id::text SIMILAR TO '(a1000001|a2000002|a3000003|a4000004|a5000005)%'
  );

  DELETE FROM inventory
  WHERE product_id::text SIMILAR TO '(a1000001|a2000002|a3000003|a4000004|a5000005)%';

  DELETE FROM product_images
  WHERE product_id::text SIMILAR TO '(a1000001|a2000002|a3000003|a4000004|a5000005)%';

  DELETE FROM products
  WHERE id::text SIMILAR TO '(a1000001|a2000002|a3000003|a4000004|a5000005)%';

END $$;
