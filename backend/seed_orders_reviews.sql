-- =====================================================================
-- SEED: Órdenes, Reseñas y Compradores para NeoGaming
-- 2 compradores, 6 órdenes (distintos estados), grupos, ítems y 4 reseñas
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Limpiar solo tablas de órdenes y reseñas (no toca productos/vendedores)
TRUNCATE TABLE reviews, order_items, order_groups, orders, addresses RESTART IDENTITY CASCADE;

DO $$
DECLARE
  -- Compradores
  buyer1  UUID := gen_random_uuid();
  buyer2  UUID := gen_random_uuid();

  -- Vendedores (leídos de la BD)
  sel1 UUID; sel2 UUID; sel3 UUID; sel4 UUID; sel5 UUID;

  -- IDs de órdenes
  ord1 UUID; ord2 UUID; ord3 UUID;
  ord4 UUID; ord5 UUID; ord6 UUID;

  -- IDs reutilizables para grupos/items
  grp_id UUID;

  -- Registros de producto (id, name, sku, precio con IVA)
  r1 RECORD; r2 RECORD; r3 RECORD;

  -- Subtotales calculados
  sub1 NUMERIC; sub2 NUMERIC; sub_total NUMERIC;

  -- Snapshots de dirección de envío (JSONB)
  addr_bogota JSONB;
  addr_medellin JSONB;

BEGIN

  -- ================================================================
  -- Leer IDs de vendedores ya existentes
  -- ================================================================
  SELECT id INTO sel1 FROM sellers WHERE store_slug = 'techstore-colombia';
  SELECT id INTO sel2 FROM sellers WHERE store_slug = 'gamer-paradise';
  SELECT id INTO sel3 FROM sellers WHERE store_slug = 'pc-master-race-co';
  SELECT id INTO sel4 FROM sellers WHERE store_slug = 'nextgen-gaming';
  SELECT id INTO sel5 FROM sellers WHERE store_slug = 'perifericos-pro';

  -- ================================================================
  -- 1. COMPRADORES
  -- ================================================================
  INSERT INTO users (id, email, password_hash, first_name, last_name, phone, role, status, email_verified, created_at, updated_at)
  VALUES (buyer1, 'sofia.buyer@neogaming.co', crypt('Password123!', gen_salt('bf',10)),
          'Sofia','Ramirez','3105551234','CLIENT','ACTIVE',TRUE, NOW(), NOW());

  INSERT INTO users (id, email, password_hash, first_name, last_name, phone, role, status, email_verified, created_at, updated_at)
  VALUES (buyer2, 'miguel.buyer@neogaming.co', crypt('Password123!', gen_salt('bf',10)),
          'Miguel','Torres','3145557890','CLIENT','ACTIVE',TRUE, NOW(), NOW());

  -- ================================================================
  -- 2. DIRECCIONES GUARDADAS DE LOS COMPRADORES
  -- ================================================================
  INSERT INTO addresses (id, user_id, label, street, number, apartment, city, department, country, postal_code, is_primary, status, created_at, updated_at)
  VALUES (gen_random_uuid(), buyer1, 'Casa', 'Cra 7', '# 32-15', '301', 'Bogotá', 'Cundinamarca', 'Colombia', '110231', TRUE, 'ACTIVE', NOW(), NOW());

  INSERT INTO addresses (id, user_id, label, street, number, city, department, country, postal_code, is_primary, status, created_at, updated_at)
  VALUES (gen_random_uuid(), buyer1, 'Oficina', 'Calle 72', '# 10-07', 'Bogotá', 'Cundinamarca', 'Colombia', '110221', FALSE, 'ACTIVE', NOW(), NOW());

  INSERT INTO addresses (id, user_id, label, street, number, apartment, city, department, country, postal_code, is_primary, status, created_at, updated_at)
  VALUES (gen_random_uuid(), buyer2, 'Casa', 'Calle 50', '# 43-50', '802', 'Medellín', 'Antioquia', 'Colombia', '050024', TRUE, 'ACTIVE', NOW(), NOW());

  -- Snapshots de dirección para los pedidos (copia inmutable del momento de compra)
  addr_bogota   := '{"label":"Casa","street":"Cra 7","number":"# 32-15","apartment":"301","city":"Bogotá","department":"Cundinamarca","country":"Colombia","postalCode":"110231"}'::JSONB;
  addr_medellin := '{"label":"Casa","street":"Calle 50","number":"# 43-50","apartment":"802","city":"Medellín","department":"Antioquia","country":"Colombia","postalCode":"050024"}'::JSONB;

  -- ================================================================
  -- ÓRDENES DE SOFIA (buyer1)
  -- ================================================================

  -- ── Orden 1: DELIVERED ── TechStore Colombia ─────────────────────
  ord1 := gen_random_uuid();
  SELECT id, name, sku, ROUND(base_price * 1.19, 0) AS price INTO r1 FROM products WHERE seller_id = sel1 ORDER BY name LIMIT 1 OFFSET 0;
  SELECT id, name, sku, ROUND(base_price * 1.19, 0) AS price INTO r2 FROM products WHERE seller_id = sel1 ORDER BY name LIMIT 1 OFFSET 1;
  sub1 := r1.price + r2.price;

  INSERT INTO orders (id, user_id, status, shipping_address, subtotal, shipping_cost, total, created_at, updated_at)
  VALUES (ord1, buyer1, 'DELIVERED', addr_bogota, sub1, 0, sub1,
          NOW() - INTERVAL '20 days', NOW() - INTERVAL '8 days');

  grp_id := gen_random_uuid();
  INSERT INTO order_groups (id, order_id, seller_id, status, subtotal, tracking_number, created_at, updated_at)
  VALUES (grp_id, ord1, sel1, 'DELIVERED', sub1, 'TRC-2025-0091',
          NOW() - INTERVAL '20 days', NOW() - INTERVAL '8 days');

  INSERT INTO order_items (id, order_id, order_group_id, product_id, product_name, product_sku, quantity, unit_price, subtotal, created_at)
  VALUES (gen_random_uuid(), ord1, grp_id, r1.id, r1.name, r1.sku, 1, r1.price, r1.price, NOW() - INTERVAL '20 days');

  INSERT INTO order_items (id, order_id, order_group_id, product_id, product_name, product_sku, quantity, unit_price, subtotal, created_at)
  VALUES (gen_random_uuid(), ord1, grp_id, r2.id, r2.name, r2.sku, 1, r2.price, r2.price, NOW() - INTERVAL '20 days');

  -- Reseñas de la orden 1 (ambas APROBADAS)
  INSERT INTO reviews (id, product_id, user_id, order_id, rating, title, body, status, created_at, updated_at)
  VALUES (gen_random_uuid(), r1.id, buyer1, ord1, 5,
          'Excelente producto, muy recomendado',
          'Llegó en perfectas condiciones y en tiempo récord. Calidad premium, exactamente como se describe. Totalmente recomendado para gamers exigentes.',
          'APPROVED', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days');

  INSERT INTO reviews (id, product_id, user_id, order_id, rating, title, body, status, created_at, updated_at)
  VALUES (gen_random_uuid(), r2.id, buyer1, ord1, 4,
          'Muy buena relación calidad precio',
          'Buen producto, cumple con lo prometido. La entrega fue rápida y el empaque estaba impecable. Le falta un poco de acabado pero en general muy satisfecho.',
          'APPROVED', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days');

  -- ── Orden 2: SHIPPED ── Gamer Paradise ───────────────────────────
  ord2 := gen_random_uuid();
  SELECT id, name, sku, ROUND(base_price * 1.19, 0) AS price INTO r1 FROM products WHERE seller_id = sel2 ORDER BY name LIMIT 1 OFFSET 0;
  SELECT id, name, sku, ROUND(base_price * 1.19, 0) AS price INTO r2 FROM products WHERE seller_id = sel2 ORDER BY name LIMIT 1 OFFSET 1;
  sub1 := r1.price + r2.price;

  INSERT INTO orders (id, user_id, status, shipping_address, subtotal, shipping_cost, total, created_at, updated_at)
  VALUES (ord2, buyer1, 'SHIPPED', addr_bogota, sub1, 0, sub1,
          NOW() - INTERVAL '6 days', NOW() - INTERVAL '3 days');

  grp_id := gen_random_uuid();
  INSERT INTO order_groups (id, order_id, seller_id, status, subtotal, tracking_number, created_at, updated_at)
  VALUES (grp_id, ord2, sel2, 'SHIPPED', sub1, 'TRC-2025-0124',
          NOW() - INTERVAL '6 days', NOW() - INTERVAL '3 days');

  INSERT INTO order_items (id, order_id, order_group_id, product_id, product_name, product_sku, quantity, unit_price, subtotal, created_at)
  VALUES (gen_random_uuid(), ord2, grp_id, r1.id, r1.name, r1.sku, 1, r1.price, r1.price, NOW() - INTERVAL '6 days');

  INSERT INTO order_items (id, order_id, order_group_id, product_id, product_name, product_sku, quantity, unit_price, subtotal, created_at)
  VALUES (gen_random_uuid(), ord2, grp_id, r2.id, r2.name, r2.sku, 1, r2.price, r2.price, NOW() - INTERVAL '6 days');

  -- ── Orden 3: PROCESSING ── Multivendedor (sel3 + sel4) ───────────
  ord3 := gen_random_uuid();
  SELECT id, name, sku, ROUND(base_price * 1.19, 0) AS price INTO r1 FROM products WHERE seller_id = sel3 ORDER BY name LIMIT 1 OFFSET 0;
  SELECT id, name, sku, ROUND(base_price * 1.19, 0) AS price INTO r2 FROM products WHERE seller_id = sel3 ORDER BY name LIMIT 1 OFFSET 1;
  SELECT id, name, sku, ROUND(base_price * 1.19, 0) AS price INTO r3 FROM products WHERE seller_id = sel4 ORDER BY name LIMIT 1 OFFSET 0;
  sub1      := r1.price * 2 + r2.price;        -- grupo sel3
  sub2      := r3.price;                        -- grupo sel4
  sub_total := sub1 + sub2;

  INSERT INTO orders (id, user_id, status, shipping_address, subtotal, shipping_cost, total, created_at, updated_at)
  VALUES (ord3, buyer1, 'PROCESSING', addr_bogota, sub_total, 0, sub_total,
          NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day');

  -- Grupo del vendedor 3
  grp_id := gen_random_uuid();
  INSERT INTO order_groups (id, order_id, seller_id, status, subtotal, created_at, updated_at)
  VALUES (grp_id, ord3, sel3, 'PREPARING', sub1, NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day');

  INSERT INTO order_items (id, order_id, order_group_id, product_id, product_name, product_sku, quantity, unit_price, subtotal, created_at)
  VALUES (gen_random_uuid(), ord3, grp_id, r1.id, r1.name, r1.sku, 2, r1.price, r1.price * 2, NOW() - INTERVAL '2 days');

  INSERT INTO order_items (id, order_id, order_group_id, product_id, product_name, product_sku, quantity, unit_price, subtotal, created_at)
  VALUES (gen_random_uuid(), ord3, grp_id, r2.id, r2.name, r2.sku, 1, r2.price, r2.price, NOW() - INTERVAL '2 days');

  -- Grupo del vendedor 4
  grp_id := gen_random_uuid();
  INSERT INTO order_groups (id, order_id, seller_id, status, subtotal, created_at, updated_at)
  VALUES (grp_id, ord3, sel4, 'CONFIRMED', sub2, NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day');

  INSERT INTO order_items (id, order_id, order_group_id, product_id, product_name, product_sku, quantity, unit_price, subtotal, created_at)
  VALUES (gen_random_uuid(), ord3, grp_id, r3.id, r3.name, r3.sku, 1, r3.price, r3.price, NOW() - INTERVAL '2 days');

  -- ================================================================
  -- ÓRDENES DE MIGUEL (buyer2)
  -- ================================================================

  -- ── Orden 4: DELIVERED ── Periféricos Pro ────────────────────────
  ord4 := gen_random_uuid();
  SELECT id, name, sku, ROUND(base_price * 1.19, 0) AS price INTO r1 FROM products WHERE seller_id = sel5 ORDER BY name LIMIT 1 OFFSET 0;
  SELECT id, name, sku, ROUND(base_price * 1.19, 0) AS price INTO r2 FROM products WHERE seller_id = sel5 ORDER BY name LIMIT 1 OFFSET 1;
  sub1 := r1.price + r2.price * 2;

  INSERT INTO orders (id, user_id, status, shipping_address, subtotal, shipping_cost, total, created_at, updated_at)
  VALUES (ord4, buyer2, 'DELIVERED', addr_medellin, sub1, 0, sub1,
          NOW() - INTERVAL '30 days', NOW() - INTERVAL '15 days');

  grp_id := gen_random_uuid();
  INSERT INTO order_groups (id, order_id, seller_id, status, subtotal, tracking_number, created_at, updated_at)
  VALUES (grp_id, ord4, sel5, 'DELIVERED', sub1, 'TRC-2025-0058',
          NOW() - INTERVAL '30 days', NOW() - INTERVAL '15 days');

  INSERT INTO order_items (id, order_id, order_group_id, product_id, product_name, product_sku, quantity, unit_price, subtotal, created_at)
  VALUES (gen_random_uuid(), ord4, grp_id, r1.id, r1.name, r1.sku, 1, r1.price, r1.price, NOW() - INTERVAL '30 days');

  INSERT INTO order_items (id, order_id, order_group_id, product_id, product_name, product_sku, quantity, unit_price, subtotal, created_at)
  VALUES (gen_random_uuid(), ord4, grp_id, r2.id, r2.name, r2.sku, 2, r2.price, r2.price * 2, NOW() - INTERVAL '30 days');

  -- Reseñas de la orden 4 (APPROVED + PENDING)
  INSERT INTO reviews (id, product_id, user_id, order_id, rating, title, body, status, created_at, updated_at)
  VALUES (gen_random_uuid(), r1.id, buyer2, ord4, 5,
          'Lo mejor que he comprado en NeoGaming',
          'Increíble calidad, superó mis expectativas. El envío llegó antes de lo esperado y el empaque perfectamente protegido. Definitivamente volveré a comprar.',
          'APPROVED', NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days');

  INSERT INTO reviews (id, product_id, user_id, order_id, rating, title, body, status, created_at, updated_at)
  VALUES (gen_random_uuid(), r2.id, buyer2, ord4, 3,
          'Bueno pero esperaba un poco más',
          'El producto es funcional y cumple su función, pero la calidad de los materiales podría ser mejor para el precio. Nada que reclamar, simplemente esperaba más.',
          'PENDING', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days');

  -- ── Orden 5: SHIPPED ── TechStore Colombia ───────────────────────
  ord5 := gen_random_uuid();
  SELECT id, name, sku, ROUND(base_price * 1.19, 0) AS price INTO r1 FROM products WHERE seller_id = sel1 ORDER BY name LIMIT 1 OFFSET 2;
  SELECT id, name, sku, ROUND(base_price * 1.19, 0) AS price INTO r2 FROM products WHERE seller_id = sel1 ORDER BY name LIMIT 1 OFFSET 3;
  sub1 := r1.price + r2.price;

  INSERT INTO orders (id, user_id, status, shipping_address, subtotal, shipping_cost, total, created_at, updated_at)
  VALUES (ord5, buyer2, 'SHIPPED', addr_medellin, sub1, 0, sub1,
          NOW() - INTERVAL '8 days', NOW() - INTERVAL '4 days');

  grp_id := gen_random_uuid();
  INSERT INTO order_groups (id, order_id, seller_id, status, subtotal, tracking_number, created_at, updated_at)
  VALUES (grp_id, ord5, sel1, 'SHIPPED', sub1, 'TRC-2025-0171',
          NOW() - INTERVAL '8 days', NOW() - INTERVAL '4 days');

  INSERT INTO order_items (id, order_id, order_group_id, product_id, product_name, product_sku, quantity, unit_price, subtotal, created_at)
  VALUES (gen_random_uuid(), ord5, grp_id, r1.id, r1.name, r1.sku, 1, r1.price, r1.price, NOW() - INTERVAL '8 days');

  INSERT INTO order_items (id, order_id, order_group_id, product_id, product_name, product_sku, quantity, unit_price, subtotal, created_at)
  VALUES (gen_random_uuid(), ord5, grp_id, r2.id, r2.name, r2.sku, 1, r2.price, r2.price, NOW() - INTERVAL '8 days');

  -- ── Orden 6: PROCESSING ── Gamer Paradise ────────────────────────
  ord6 := gen_random_uuid();
  SELECT id, name, sku, ROUND(base_price * 1.19, 0) AS price INTO r1 FROM products WHERE seller_id = sel2 ORDER BY name LIMIT 1 OFFSET 2;
  sub1 := r1.price * 3;

  INSERT INTO orders (id, user_id, status, shipping_address, subtotal, shipping_cost, total, created_at, updated_at)
  VALUES (ord6, buyer2, 'PROCESSING', addr_medellin, sub1, 0, sub1,
          NOW() - INTERVAL '1 day', NOW());

  grp_id := gen_random_uuid();
  INSERT INTO order_groups (id, order_id, seller_id, status, subtotal, created_at, updated_at)
  VALUES (grp_id, ord6, sel2, 'CONFIRMED', sub1, NOW() - INTERVAL '1 day', NOW());

  INSERT INTO order_items (id, order_id, order_group_id, product_id, product_name, product_sku, quantity, unit_price, subtotal, created_at)
  VALUES (gen_random_uuid(), ord6, grp_id, r1.id, r1.name, r1.sku, 3, r1.price, r1.price * 3, NOW() - INTERVAL '1 day');

  RAISE NOTICE 'Seed órdenes completado: 2 compradores, 3 direcciones, 6 órdenes, grupos, ítems y 4 reseñas.';
END $$;
