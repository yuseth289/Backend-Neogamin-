-- Habilitar extensión pgcrypto para crypt() y gen_salt()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =====================================================================
-- SEED DATA: NeoGaming Marketplace
-- Categorías, 1 admin, 5 vendedores, 50 productos c/u (250 total)
-- UUIDs generados explícitamente (Hibernate 7 usa GenerationType.UUID
-- que asigna el UUID en Java, no en la BD — raw SQL debe proveerlo)
-- =====================================================================

-- Limpiar datos previos para poder re-ejecutar el script
TRUNCATE TABLE inventory, products, sellers, users, categories RESTART IDENTITY CASCADE;

DO $$
DECLARE
  -- Categorías padre
  cat_consolas     UUID := gen_random_uuid();
  cat_perifericos  UUID := gen_random_uuid();
  cat_componentes  UUID := gen_random_uuid();
  cat_monitores    UUID := gen_random_uuid();
  cat_sillas       UUID := gen_random_uuid();
  cat_juegos       UUID := gen_random_uuid();
  cat_streaming    UUID := gen_random_uuid();

  -- Subcategorías
  cat_ps           UUID := gen_random_uuid();
  cat_xbox         UUID := gen_random_uuid();
  cat_switch       UUID := gen_random_uuid();
  cat_teclados     UUID := gen_random_uuid();
  cat_mouses       UUID := gen_random_uuid();
  cat_audifonos    UUID := gen_random_uuid();
  cat_gpus         UUID := gen_random_uuid();
  cat_cpus         UUID := gen_random_uuid();
  cat_ram          UUID := gen_random_uuid();

  -- Usuarios
  usr1 UUID := gen_random_uuid();
  usr2 UUID := gen_random_uuid();
  usr3 UUID := gen_random_uuid();
  usr4 UUID := gen_random_uuid();
  usr5 UUID := gen_random_uuid();

  -- Sellers
  sel1 UUID := gen_random_uuid();
  sel2 UUID := gen_random_uuid();
  sel3 UUID := gen_random_uuid();
  sel4 UUID := gen_random_uuid();
  sel5 UUID := gen_random_uuid();

  -- Temporales
  cur_seller    UUID;
  cur_prefix    TEXT;
  prod_id       UUID;
  cur_cat       UUID;
  i INTEGER; j INTEGER;

  -- Arrays de datos de productos (50 productos)
  pnames  TEXT[];
  pbrands TEXT[];
  pprices NUMERIC[];

BEGIN

  -- =================================================================
  -- 1. CATEGORÍAS PADRE
  -- =================================================================
  INSERT INTO categories (id, name, slug, description, status, created_at, updated_at) VALUES
    (cat_consolas,   'Consolas y Videojuegos', 'consolas-videojuegos', 'Consolas PS5, Xbox, Nintendo y sus accesorios', 'ACTIVE', NOW(), NOW());

  INSERT INTO categories (id, name, slug, description, status, created_at, updated_at) VALUES
    (cat_perifericos,'Periféricos Gaming',     'perifericos-gaming',   'Teclados, mouses, audífonos y accesorios de alta gama', 'ACTIVE', NOW(), NOW());

  INSERT INTO categories (id, name, slug, description, status, created_at, updated_at) VALUES
    (cat_componentes,'Componentes PC',         'componentes-pc',       'GPUs, CPUs, RAM y almacenamiento para tu build', 'ACTIVE', NOW(), NOW());

  INSERT INTO categories (id, name, slug, description, status, created_at, updated_at) VALUES
    (cat_monitores,  'Monitores',              'monitores',            'Monitores gaming 144Hz, 240Hz, 4K y OLED', 'ACTIVE', NOW(), NOW());

  INSERT INTO categories (id, name, slug, description, status, created_at, updated_at) VALUES
    (cat_sillas,     'Sillas y Escritorios',   'sillas-escritorios',   'Ergonomia y comodidad para largas sesiones', 'ACTIVE', NOW(), NOW());

  INSERT INTO categories (id, name, slug, description, status, created_at, updated_at) VALUES
    (cat_juegos,     'Juegos',                 'juegos',               'Juegos fisicos para PS5, Xbox y Nintendo', 'ACTIVE', NOW(), NOW());

  INSERT INTO categories (id, name, slug, description, status, created_at, updated_at) VALUES
    (cat_streaming,  'Streaming y Contenido',  'streaming-contenido',  'Equipos profesionales para streamers y creadores', 'ACTIVE', NOW(), NOW());

  -- =================================================================
  -- 2. SUBCATEGORÍAS
  -- =================================================================
  INSERT INTO categories (id, name, slug, description, parent_id, status, created_at, updated_at) VALUES
    (cat_ps,      'PlayStation',       'playstation',      'Consolas PS4, PS5 y accesorios Sony',             cat_consolas,    'ACTIVE', NOW(), NOW());

  INSERT INTO categories (id, name, slug, description, parent_id, status, created_at, updated_at) VALUES
    (cat_xbox,    'Xbox',              'xbox',             'Xbox Series X/S y accesorios Microsoft',          cat_consolas,    'ACTIVE', NOW(), NOW());

  INSERT INTO categories (id, name, slug, description, parent_id, status, created_at, updated_at) VALUES
    (cat_switch,  'Nintendo Switch',   'nintendo-switch',  'Switch OLED, Lite y juegos Nintendo',             cat_consolas,    'ACTIVE', NOW(), NOW());

  INSERT INTO categories (id, name, slug, description, parent_id, status, created_at, updated_at) VALUES
    (cat_teclados,'Teclados Gaming',   'teclados-gaming',  'Teclados mecanicos, opticos y de membrana',       cat_perifericos, 'ACTIVE', NOW(), NOW());

  INSERT INTO categories (id, name, slug, description, parent_id, status, created_at, updated_at) VALUES
    (cat_mouses,  'Mouses Gaming',     'mouses-gaming',    'Mouses de alta precision con sensores gaming',    cat_perifericos, 'ACTIVE', NOW(), NOW());

  INSERT INTO categories (id, name, slug, description, parent_id, status, created_at, updated_at) VALUES
    (cat_audifonos,'Audifonos Gaming', 'audifonos-gaming', 'Headsets con sonido envolvente 7.1',              cat_perifericos, 'ACTIVE', NOW(), NOW());

  INSERT INTO categories (id, name, slug, description, parent_id, status, created_at, updated_at) VALUES
    (cat_gpus,    'Tarjetas Graficas', 'tarjetas-graficas','GPUs NVIDIA y AMD para gaming y trabajo',         cat_componentes, 'ACTIVE', NOW(), NOW());

  INSERT INTO categories (id, name, slug, description, parent_id, status, created_at, updated_at) VALUES
    (cat_cpus,    'Procesadores',      'procesadores',     'CPUs Intel Core y AMD Ryzen de ultima generacion', cat_componentes,'ACTIVE', NOW(), NOW());

  INSERT INTO categories (id, name, slug, description, parent_id, status, created_at, updated_at) VALUES
    (cat_ram,     'Memoria RAM',       'memoria-ram',      'Kits DDR4 y DDR5 para gaming',                    cat_componentes, 'ACTIVE', NOW(), NOW());

  -- =================================================================
  -- 3. ADMIN
  -- =================================================================
  INSERT INTO users (id, email, password_hash, first_name, last_name, role, status, email_verified, created_at, updated_at)
  VALUES (gen_random_uuid(), 'admin@neogaming.co', crypt('Admin123!', gen_salt('bf',10)),
          'Admin', 'NeoGaming', 'ADMIN', 'ACTIVE', TRUE, NOW(), NOW());

  -- =================================================================
  -- 4. USUARIOS VENDEDORES
  -- =================================================================
  INSERT INTO users (id, email, password_hash, first_name, last_name, phone, role, status, email_verified, created_at, updated_at)
  VALUES (usr1, 'andres.tech@neogaming.co', crypt('Password123!', gen_salt('bf',10)),
          'Andres','Martinez','3001234567','SELLER','ACTIVE',TRUE, NOW(), NOW());

  INSERT INTO users (id, email, password_hash, first_name, last_name, phone, role, status, email_verified, created_at, updated_at)
  VALUES (usr2, 'maria.games@neogaming.co', crypt('Password123!', gen_salt('bf',10)),
          'Maria','Gonzalez','3109876543','SELLER','ACTIVE',TRUE, NOW(), NOW());

  INSERT INTO users (id, email, password_hash, first_name, last_name, phone, role, status, email_verified, created_at, updated_at)
  VALUES (usr3, 'carlos.components@neogaming.co', crypt('Password123!', gen_salt('bf',10)),
          'Carlos','Rodriguez','3157654321','SELLER','ACTIVE',TRUE, NOW(), NOW());

  INSERT INTO users (id, email, password_hash, first_name, last_name, phone, role, status, email_verified, created_at, updated_at)
  VALUES (usr4, 'laura.gaming@neogaming.co', crypt('Password123!', gen_salt('bf',10)),
          'Laura','Perez','3204567890','SELLER','ACTIVE',TRUE, NOW(), NOW());

  INSERT INTO users (id, email, password_hash, first_name, last_name, phone, role, status, email_verified, created_at, updated_at)
  VALUES (usr5, 'juan.perifericos@neogaming.co', crypt('Password123!', gen_salt('bf',10)),
          'Juan','Lopez','3012345678','SELLER','ACTIVE',TRUE, NOW(), NOW());

  -- =================================================================
  -- 5. PERFILES DE VENDEDOR
  -- =================================================================
  INSERT INTO sellers (id, user_id, store_name, store_slug, store_description,
    tipo_documento, numero_documento, tipo_regimen, status, phone, city, department, created_at, updated_at)
  VALUES (sel1, usr1, 'TechStore Colombia','techstore-colombia',
    'Los mejores perifericos y componentes PC al mejor precio en Colombia',
    'NIT','900123456-1','RESPONSABLE_IVA','ACTIVE','3001234567','Bogota','Cundinamarca', NOW(), NOW());

  INSERT INTO sellers (id, user_id, store_name, store_slug, store_description,
    tipo_documento, numero_documento, tipo_regimen, status, phone, city, department, created_at, updated_at)
  VALUES (sel2, usr2, 'Gamer Paradise','gamer-paradise',
    'Tu tienda gamer de confianza: consolas, juegos y accesorios originales',
    'CC','52345678','NO_RESPONSABLE_IVA','ACTIVE','3109876543','Medellin','Antioquia', NOW(), NOW());

  INSERT INTO sellers (id, user_id, store_name, store_slug, store_description,
    tipo_documento, numero_documento, tipo_regimen, status, phone, city, department, created_at, updated_at)
  VALUES (sel3, usr3, 'PC Master Race CO','pc-master-race-co',
    'Componentes de alta gama para entusiastas del PC gaming en Colombia',
    'NIT','800987654-2','RESPONSABLE_IVA','ACTIVE','3157654321','Cali','Valle del Cauca', NOW(), NOW());

  INSERT INTO sellers (id, user_id, store_name, store_slug, store_description,
    tipo_documento, numero_documento, tipo_regimen, status, phone, city, department, created_at, updated_at)
  VALUES (sel4, usr4, 'NextGen Gaming','nextgen-gaming',
    'Tecnologia de nueva generacion: consolas, accesorios y mas',
    'CC','43567890','NO_RESPONSABLE_IVA','ACTIVE','3204567890','Barranquilla','Atlantico', NOW(), NOW());

  INSERT INTO sellers (id, user_id, store_name, store_slug, store_description,
    tipo_documento, numero_documento, tipo_regimen, status, phone, city, department, created_at, updated_at)
  VALUES (sel5, usr5, 'Perifericos Pro','perifericos-pro',
    'Especialistas en perifericos gaming de alta calidad para todos los niveles',
    'NIT','700654321-3','RESPONSABLE_IVA','ACTIVE','3012345678','Bucaramanga','Santander', NOW(), NOW());

  -- =================================================================
  -- 6. PRODUCTOS (50 por vendedor)
  -- =================================================================

  pnames := ARRAY[
    'Mouse Gaming RGB 12000 DPI Logitech G502',
    'Teclado Mecanico RGB Switch Blue HyperX Alloy',
    'Headset Gaming 7.1 Surround SteelSeries Arctis',
    'Monitor Gaming 144Hz 24in Full HD IPS',
    'Silla Gamer Ergonomica con Reposapiernas DXRacer',
    'GPU NVIDIA RTX 4070 Super 12GB GDDR6X',
    'CPU Intel Core i7-14700K 20 Nucleos',
    'RAM DDR5 32GB 6000MHz Corsair Vengeance',
    'SSD NVMe M2 1TB Gen4 Samsung 990 Pro',
    'Control PS5 DualSense Blanco',
    'Control Xbox Series X Carbon Black',
    'Nintendo Switch OLED Blanco 64GB',
    'Mousepad XXL RGB Borde Cosido 900x400mm',
    'Cooler CPU Liquido 360mm AIO Corsair H150i',
    'Fuente 850W 80 Plus Gold Modular Seasonic',
    'Gabinete ATX Vidrio Templado ARGB NZXT H510',
    'Webcam 4K 60fps Logitech Brio con Microfono',
    'Microfono Condenser USB Blue Yeti Cardioide',
    'Ring Light LED 18in Neewer con Tripode',
    'Capturadora Video HDMI 4K USB-C Elgato 4K60',
    'Router WiFi 6 AX6000 Gaming ASUS ROG Rapture',
    'Hub USB-C 10 en 1 100W PD Anker PowerExpand',
    'Monitor 4K 27in IPS 144Hz LG UltraGear',
    'Mousepad Duro XL Control Speed Razer Strider',
    'Soporte Monitor Articulado VESA Ergotron LX',
    'Audifonos Bluetooth 5.3 ANC Sony WH-1000XM5',
    'Parlantes 2.1 RGB 60W RMS Logitech Z623',
    'Teclado Mecanico TKL Switch Red Ducky One 3',
    'Mouse Ergonomico Vertical 6 Botones Logitech MX',
    'Cargador Inalambrico Qi 15W Belkin BoostCharge',
    'UPS 1500VA 900W AVR APC Back-UPS',
    'SSD Externo 2TB USB-C 1000MBs Samsung T7',
    'Disco Duro Externo 4TB USB 3.0 Seagate Backup',
    'RAM DDR4 16GB 3200MHz CL16 Kingston Fury',
    'GPU AMD RX 7800 XT 16GB GDDR6',
    'CPU AMD Ryzen 7 7700X 8 Nucleos 4.5GHz',
    'Placa Base B650 Gaming WiFi ATX MSI MAG',
    'Refrigeracion CPU Dark Rock Pro 4 be quiet',
    'Mouse Gaming 25600 DPI Inalambrico Logitech G Pro X',
    'Headset Inalambrico 2.4GHz 30h Corsair HS80',
    'Teclado 65 Porciento Hot-Swap RGB Keychron K6',
    'Mousepad Control Speed Artisan Zero Soft XL',
    'Control PS5 DualSense Edge Alta Performance',
    'Juego Elden Ring PS5 From Software',
    'Juego God of War Ragnarok PS5 Sony',
    'Stream Deck MK2 15 Teclas LCD Elgato',
    'Camara Canon EOS M50 Mark II 24MP',
    'Cable HDMI 2.1 4K 120Hz 3m Certificado',
    'Soporte Headset Escritorio RGB Razer Base Station',
    'Kit Iluminacion ARGB LED Strip 2m Phanteks'
  ];

  pbrands := ARRAY[
    'Logitech','HyperX','SteelSeries','Samsung','DXRacer',
    'NVIDIA','Intel','Corsair','Samsung','Sony',
    'Microsoft','Nintendo','Razer','Corsair','Seasonic',
    'NZXT','Logitech','Blue','Neewer','Elgato',
    'ASUS','Anker','LG','Razer','Ergotron',
    'Sony','Logitech','Ducky','Logitech','Belkin',
    'APC','Samsung','Seagate','Kingston','AMD',
    'AMD','MSI','be quiet!','Logitech','Corsair',
    'Keychron','Artisan','Sony','Bandai Namco','Sony',
    'Elgato','Canon','Belkin','Razer','Phanteks'
  ];

  pprices := ARRAY[
    180000, 350000, 420000, 1200000, 950000,
    3800000, 1450000, 980000, 420000, 380000,
    360000, 1600000, 85000, 750000, 480000,
    650000, 680000, 290000, 220000, 580000,
    890000, 180000, 2400000, 95000, 320000,
    580000, 340000, 320000, 165000, 120000,
    680000, 880000, 520000, 280000, 3200000,
    1380000, 920000, 385000, 320000, 650000,
    480000, 110000, 1200000, 280000, 260000,
    780000, 2850000, 45000, 135000, 95000
  ]::NUMERIC[];

  -- Loop por los 5 vendedores
  FOR j IN 1..5 LOOP
    cur_seller := CASE j WHEN 1 THEN sel1 WHEN 2 THEN sel2 WHEN 3 THEN sel3 WHEN 4 THEN sel4 ELSE sel5 END;
    cur_prefix  := CASE j WHEN 1 THEN 'tsc' WHEN 2 THEN 'gpx' WHEN 3 THEN 'pcm' WHEN 4 THEN 'ngg' ELSE 'prp' END;

    FOR i IN 1..50 LOOP
      -- Asignar categoría según rango de producto
      cur_cat := CASE
        WHEN i IN (1,13,24,29,39,42)        THEN cat_mouses
        WHEN i IN (2,28,41)                 THEN cat_teclados
        WHEN i IN (3,26,40)                 THEN cat_audifonos
        WHEN i IN (4,23)                    THEN cat_monitores
        WHEN i IN (5,25)                    THEN cat_sillas
        WHEN i IN (6,35)                    THEN cat_gpus
        WHEN i IN (7,36)                    THEN cat_cpus
        WHEN i IN (8,34)                    THEN cat_ram
        WHEN i IN (9,15,16,31,32,33,37,38) THEN cat_componentes
        WHEN i IN (10,43)                   THEN cat_ps
        WHEN i IN (11,30)                   THEN cat_xbox
        WHEN i IN (12)                      THEN cat_switch
        WHEN i IN (44,45)                   THEN cat_juegos
        WHEN i IN (17,18,19,20,46,47)       THEN cat_streaming
        ELSE cat_perifericos
      END;

      prod_id := gen_random_uuid();

      INSERT INTO products (
        id, seller_id, category_id, name, slug, description, brand, sku,
        base_price, iva_percent, status, created_at, updated_at
      )
      VALUES (
        prod_id,
        cur_seller,
        cur_cat,
        pnames[i],
        cur_prefix || '-' || lpad(j::text,1,'0') || '-' || lpad(i::text,2,'0'),
        'Producto original con garantia de fabrica: ' || pnames[i] ||
          '. Incluye accesorios. Envio gratis a todo Colombia.',
        pbrands[i],
        upper(cur_prefix) || '-' || lpad(i::text,3,'0') || '-V' || j,
        pprices[i] * (0.9 + (((j-1)*50 + i) % 5) * 0.05),
        19.00,
        'ACTIVE',
        NOW(), NOW()
      );

      -- Inventario para cada producto
      INSERT INTO inventory (id, product_id, physical_stock, reserved_stock, created_at, updated_at)
      VALUES (gen_random_uuid(), prod_id, 20 + (i % 80), 0, NOW(), NOW());

    END LOOP;
  END LOOP;

  RAISE NOTICE 'Seed completado: 16 categorias, 1 admin, 5 vendedores, 250 productos con inventario.';
END $$;
