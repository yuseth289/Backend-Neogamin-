-- ============================================================
-- V18: SEED DE DATOS PARA DEMO
-- Limpia productos anteriores (sin imágenes) y puebla la
-- plataforma con 50 productos realistas, imágenes Unsplash,
-- inventario, reseñas y ofertas activas.
-- ============================================================

-- Permite reseñas sin orden (demo/seed de admin)
ALTER TABLE reviews ALTER COLUMN order_id DROP NOT NULL;

DO $$
DECLARE
  -- ── SELLERS ──────────────────────────────────────────────
  seller_tc  UUID := '30cf680f-085d-47fc-9069-ac9e8e2bc4d3'; -- TechStore Colombia
  seller_pm  UUID := '4f68b4e1-1f6a-455f-918f-09c9da612947'; -- PC Master Race CO
  seller_gp  UUID := 'e9d27cea-b333-4ea3-9524-bb66c75d36ce'; -- Gamer Paradise
  seller_ng  UUID := 'f98da79a-bf32-4428-b175-cc6b62470088'; -- NextGen Gaming
  seller_pp  UUID := '8f6d04ac-3329-40ff-aea9-1648660dc35f'; -- Perifericos Pro

  -- ── BUYERS ───────────────────────────────────────────────
  buyer1     UUID := '630b0823-b2c0-4cb6-bf43-26f6c6dbe76c'; -- miguel.buyer
  buyer2     UUID := '2b3c37ad-80af-4f10-8f6d-48050a49a937'; -- sofia.buyer

  -- ── CATEGORÍAS ───────────────────────────────────────────
  cat_gpu    UUID := '4db9df30-c721-4a66-81f5-9e2da057fa75'; -- Tarjetas Graficas
  cat_cpu    UUID := '09e9f47f-6c37-4dcc-8109-83c0134d64b8'; -- Procesadores
  cat_comp   UUID := '04b0f90c-3806-4e97-b522-4cd3ea8a2d1f'; -- Componentes PC
  cat_ram    UUID := '232245b2-10f1-4bfb-93be-80f646e6838b'; -- Memoria RAM
  cat_mon    UUID := 'a25c296a-f1cc-4e70-af58-ffd2283ef3d2'; -- Monitores
  cat_mouse  UUID := 'e2be7b93-2697-475b-90a7-c4a85c2d69db'; -- Mouses Gaming
  cat_key    UUID := 'fd69f133-bb23-4cdc-92ae-976834f90e48'; -- Teclados Gaming
  cat_head   UUID := 'cc8e0ad3-4130-46aa-82a5-71f0eaed169c'; -- Audifonos Gaming
  cat_peri   UUID := '975cb5e0-176a-42d5-bf9d-2e6c466ba33b'; -- Periféricos Gaming
  cat_cons   UUID := 'b8378b6e-fdc6-4ade-8388-204a54074926'; -- Consolas y Videojuegos
  cat_ps     UUID := '9260fa80-483b-4f8b-87f1-ac8b89712466'; -- PlayStation
  cat_xbox   UUID := 'b938bcc2-2aa3-413c-a606-ac60212f8026'; -- Xbox
  cat_sw     UUID := 'f576dee8-1d53-4671-a9c5-e20d97b4175e'; -- Nintendo Switch
  cat_games  UUID := '98a3dde1-5c92-44e5-a420-9e1c17af03ec'; -- Juegos
  cat_stream UUID := '2466d786-d1f4-4c02-8b2b-efb903e7631d'; -- Streaming y Contenido
  cat_silla  UUID := 'd7d84f96-418e-48f2-82d9-cf2c966ce828'; -- Sillas y Escritorios

  -- ── PRODUCT IDs (TechStore Colombia) ─────────────────────
  p_rtx4070  UUID := 'a1000001-0000-0000-0000-000000000001';
  p_rx7800   UUID := 'a1000001-0000-0000-0000-000000000002';
  p_i7_14k   UUID := 'a1000001-0000-0000-0000-000000000003';
  p_r7_7700x UUID := 'a1000001-0000-0000-0000-000000000004';
  p_b650     UUID := 'a1000001-0000-0000-0000-000000000005';
  p_ddr5_32  UUID := 'a1000001-0000-0000-0000-000000000006';
  p_ddr4_16  UUID := 'a1000001-0000-0000-0000-000000000007';
  p_990pro   UUID := 'a1000001-0000-0000-0000-000000000008';
  p_seasonic UUID := 'a1000001-0000-0000-0000-000000000009';
  p_darkrock UUID := 'a1000001-0000-0000-0000-000000000010';

  -- ── PRODUCT IDs (PC Master Race CO) ──────────────────────
  p_lg4k     UUID := 'a2000002-0000-0000-0000-000000000001';
  p_mon144   UUID := 'a2000002-0000-0000-0000-000000000002';
  p_nzxt     UUID := 'a2000002-0000-0000-0000-000000000003';
  p_h150i    UUID := 'a2000002-0000-0000-0000-000000000004';
  p_argbled  UUID := 'a2000002-0000-0000-0000-000000000005';
  p_ssd2tb   UUID := 'a2000002-0000-0000-0000-000000000006';
  p_hdd4tb   UUID := 'a2000002-0000-0000-0000-000000000007';
  p_ups      UUID := 'a2000002-0000-0000-0000-000000000008';
  p_hub      UUID := 'a2000002-0000-0000-0000-000000000009';
  p_qi15w    UUID := 'a2000002-0000-0000-0000-000000000010';

  -- ── PRODUCT IDs (Gamer Paradise) ─────────────────────────
  p_switch   UUID := 'a3000003-0000-0000-0000-000000000001';
  p_ds5blco  UUID := 'a3000003-0000-0000-0000-000000000002';
  p_ds5edge  UUID := 'a3000003-0000-0000-0000-000000000003';
  p_xbox_pad UUID := 'a3000003-0000-0000-0000-000000000004';
  p_elden    UUID := 'a3000003-0000-0000-0000-000000000005';
  p_gow      UUID := 'a3000003-0000-0000-0000-000000000006';
  p_dxracer  UUID := 'a3000003-0000-0000-0000-000000000007';
  p_ergotron UUID := 'a3000003-0000-0000-0000-000000000008';
  p_razerbase UUID:= 'a3000003-0000-0000-0000-000000000009';
  p_mpad_xxl UUID := 'a3000003-0000-0000-0000-000000000010';

  -- ── PRODUCT IDs (NextGen Gaming) ─────────────────────────
  p_brio     UUID := 'a4000004-0000-0000-0000-000000000001';
  p_yeti     UUID := 'a4000004-0000-0000-0000-000000000002';
  p_streamdk UUID := 'a4000004-0000-0000-0000-000000000003';
  p_elgato4k UUID := 'a4000004-0000-0000-0000-000000000004';
  p_ringlt   UUID := 'a4000004-0000-0000-0000-000000000005';
  p_canonm50 UUID := 'a4000004-0000-0000-0000-000000000006';
  p_asusrog  UUID := 'a4000004-0000-0000-0000-000000000007';
  p_z623     UUID := 'a4000004-0000-0000-0000-000000000008';
  p_hdmi21   UUID := 'a4000004-0000-0000-0000-000000000009';
  p_sony_wh  UUID := 'a4000004-0000-0000-0000-000000000010';

  -- ── PRODUCT IDs (Perifericos Pro) ────────────────────────
  p_gpro     UUID := 'a5000005-0000-0000-0000-000000000001';
  p_g502     UUID := 'a5000005-0000-0000-0000-000000000002';
  p_mxvert   UUID := 'a5000005-0000-0000-0000-000000000003';
  p_hyper    UUID := 'a5000005-0000-0000-0000-000000000004';
  p_k6       UUID := 'a5000005-0000-0000-0000-000000000005';
  p_ducky    UUID := 'a5000005-0000-0000-0000-000000000006';
  p_strider  UUID := 'a5000005-0000-0000-0000-000000000007';
  p_artisan  UUID := 'a5000005-0000-0000-0000-000000000008';
  p_arctis   UUID := 'a5000005-0000-0000-0000-000000000009';
  p_hs80     UUID := 'a5000005-0000-0000-0000-000000000010';

  -- ── IMÁGENES BASE (Unsplash, categorías gaming/tech) ─────
  img_gpu    TEXT := 'https://images.unsplash.com/photo-1591488320131-5fcf40e3e6cd?w=800&h=800&fit=crop&q=80';
  img_gpu2   TEXT := 'https://images.unsplash.com/photo-1555680202-c86f0e12f086?w=800&h=800&fit=crop&q=80';
  img_cpu    TEXT := 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&h=800&fit=crop&q=80';
  img_mobo   TEXT := 'https://images.unsplash.com/photo-1597872200666-08b4b3b7c5c9?w=800&h=800&fit=crop&q=80';
  img_ram    TEXT := 'https://images.unsplash.com/photo-1562976540-1502c2145851?w=800&h=800&fit=crop&q=80';
  img_ssd    TEXT := 'https://images.unsplash.com/photo-1624638991956-f98cbf7ce1a4?w=800&h=800&fit=crop&q=80';
  img_case_  TEXT := 'https://images.unsplash.com/photo-1587202372634-32705e3bf49c?w=800&h=800&fit=crop&q=80';
  img_mon    TEXT := 'https://images.unsplash.com/photo-1527443224154-b6611813d2fd?w=800&h=800&fit=crop&q=80';
  img_setup  TEXT := 'https://images.unsplash.com/photo-1593640495395-cee3c6d42a3e?w=800&h=800&fit=crop&q=80';
  img_key    TEXT := 'https://images.unsplash.com/photo-1612836697765-a3881df9c2c4?w=800&h=800&fit=crop&q=80';
  img_key2   TEXT := 'https://images.unsplash.com/photo-1615655043613-db6b1b81f9b7?w=800&h=800&fit=crop&q=80';
  img_mouse  TEXT := 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=800&h=800&fit=crop&q=80';
  img_headset TEXT:= 'https://images.unsplash.com/photo-1583341617728-b8b95ef7a0b2?w=800&h=800&fit=crop&q=80';
  img_headset2 TEXT:= 'https://images.unsplash.com/photo-1545127398-14699f92334b?w=800&h=800&fit=crop&q=80';
  img_chair  TEXT := 'https://images.unsplash.com/photo-1586880244406-556ebe35f282?w=800&h=800&fit=crop&q=80';
  img_ctrl   TEXT := 'https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?w=800&h=800&fit=crop&q=80';
  img_ctrl2  TEXT := 'https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=800&h=800&fit=crop&q=80';
  img_nswitch TEXT:= 'https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=800&h=800&fit=crop&q=80';
  img_mic    TEXT := 'https://images.unsplash.com/photo-1590765585061-66b5b7d2bcfb?w=800&h=800&fit=crop&q=80';
  img_cam    TEXT := 'https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=800&h=800&fit=crop&q=80';
  img_webcam TEXT := 'https://images.unsplash.com/photo-1525385133512-2f3bdd039054?w=800&h=800&fit=crop&q=80';
  img_stream TEXT := 'https://images.unsplash.com/photo-1629654297299-c8506221f6f1?w=800&h=800&fit=crop&q=80';
  img_speaker TEXT:= 'https://images.unsplash.com/photo-1545454675-3479d89e0c8d?w=800&h=800&fit=crop&q=80';
  img_router TEXT := 'https://images.unsplash.com/photo-1544197150-b99a580bb7a8?w=800&h=800&fit=crop&q=80';
  img_mpad   TEXT := 'https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&h=800&fit=crop&q=80';
  img_light  TEXT := 'https://images.unsplash.com/photo-1612836697765-a3881df9c2c4?w=800&h=800&fit=crop&q=80';
  img_cable  TEXT := 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800&h=800&fit=crop&q=80';
  img_ups    TEXT := 'https://images.unsplash.com/photo-1573164713714-d95e436ab8d6?w=800&h=800&fit=crop&q=80';
  img_game   TEXT := 'https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800&h=800&fit=crop&q=80';

BEGIN

  -- ────────────────────────────────────────────────────────
  -- 0. LIMPIAR DATOS ANTERIORES (orden por FK)
  -- ────────────────────────────────────────────────────────
  DELETE FROM reviews;
  DELETE FROM offers;
  DELETE FROM wishlist_items;
  DELETE FROM cart_items;
  DELETE FROM checkout_items;
  DELETE FROM inventory_movements;
  DELETE FROM inventory;
  DELETE FROM product_images;
  DELETE FROM products;

  -- ────────────────────────────────────────────────────────
  -- 1. ACTUALIZAR PERFIL DE SELLERS
  -- ────────────────────────────────────────────────────────
  UPDATE sellers SET
    store_description = 'Especialistas en componentes de alto rendimiento para builders y entusiastas del PC. CPUs, GPUs, RAM y más — todo con garantía y envío a todo Colombia.',
    city = 'Bogotá', department = 'Cundinamarca'
  WHERE id = seller_tc;

  UPDATE sellers SET
    store_description = 'La tienda de monitores, almacenamiento y accesorios de escritorio más completa del país. Calidad certificada y soporte técnico especializado.',
    city = 'Medellín', department = 'Antioquia'
  WHERE id = seller_pm;

  UPDATE sellers SET
    store_description = 'Tu destino gamer para consolas, juegos y accesorios. PS5, Xbox, Nintendo y todo lo que necesitas para vivir la experiencia gaming al máximo.',
    city = 'Cali', department = 'Valle del Cauca'
  WHERE id = seller_gp;

  UPDATE sellers SET
    store_description = 'Equipos profesionales para streamers y creadores de contenido. Webcams 4K, micrófonos, capturadoras y todo para tu setup de transmisión.',
    city = 'Barranquilla', department = 'Atlántico'
  WHERE id = seller_ng;

  UPDATE sellers SET
    store_description = 'Periféricos gaming de élite: mouses, teclados mecánicos y headsets de las marcas más reconocidas del mundo esports.',
    city = 'Bogotá', department = 'Cundinamarca'
  WHERE id = seller_pp;

  -- ────────────────────────────────────────────────────────
  -- 2. PRODUCTOS — TechStore Colombia (componentes PC)
  -- ────────────────────────────────────────────────────────
  INSERT INTO products (id, seller_id, category_id, name, slug, brand, description, base_price, iva_percent, sku, status, created_at, updated_at) VALUES
  (p_rtx4070, seller_tc, cat_gpu,
   'GPU NVIDIA GeForce RTX 4070 Super 12GB GDDR6X',
   'gpu-nvidia-rtx-4070-super-12gb',
   'NVIDIA',
   'La NVIDIA RTX 4070 Super lleva el rendimiento gaming a otro nivel con 12 GB de memoria GDDR6X y arquitectura Ada Lovelace. Ray tracing de última generación, DLSS 3 con Frame Generation y soporte completo para 4K. Ideal para gaming en 1440p ultrafluid y trabajo creativo profesional.',
   3034454, 19, 'GPU-NV-4070S-12G', 'ACTIVE', NOW(), NOW()),

  (p_rx7800, seller_tc, cat_gpu,
   'GPU AMD Radeon RX 7800 XT 16GB GDDR6',
   'gpu-amd-rx-7800-xt-16gb',
   'AMD',
   'La RX 7800 XT ofrece 16 GB de VRAM GDDR6 para gaming en 1440p sin compromiso. Arquitectura RDNA 3 con soporte para DisplayPort 2.1 y hasta 240 Hz. FSR 3 para escalar rendimiento y soporte completo para ray tracing. Incluye conector de alimentación directa PCIe 5.0.',
   2420168, 19, 'GPU-AMD-7800XT-16G', 'ACTIVE', NOW(), NOW()),

  (p_i7_14k, seller_tc, cat_cpu,
   'CPU Intel Core i7-14700K 20 Núcleos hasta 5.6GHz',
   'cpu-intel-i7-14700k-20-nucleos',
   'Intel',
   'El Core i7-14700K cuenta con 20 núcleos (8P+12E) y frecuencia máxima de 5.6 GHz para gaming y productividad extrema. Sin refrigerador incluido (se requiere cooler de alto rendimiento). Compatible con placas base Z690/Z790. Soporte para DDR5 y DDR4. UHD Graphics 770 integrada.',
   1218487, 19, 'CPU-INT-I714K-20C', 'ACTIVE', NOW(), NOW()),

  (p_r7_7700x, seller_tc, cat_cpu,
   'CPU AMD Ryzen 7 7700X 8 Núcleos 5.4GHz AM5',
   'cpu-amd-ryzen-7-7700x-am5',
   'AMD',
   'El Ryzen 7 7700X ofrece 8 núcleos y 16 hilos con tecnología Zen 4 en socket AM5. Frecuencia boost de 5.4 GHz, soporte nativo para DDR5 y PCIe 5.0. Arquitectura de 5nm para máxima eficiencia energética. Compatible con enfriadores AM4 con adaptador. TDP de 105W.',
   1101681, 19, 'CPU-AMD-R77700X-AM5', 'ACTIVE', NOW(), NOW()),

  (p_b650, seller_tc, cat_comp,
   'Placa Base MSI MAG B650 Tomahawk WiFi ATX AM5',
   'placa-msi-mag-b650-tomahawk-wifi',
   'MSI',
   'Diseñada para Ryzen 7000 en socket AM5, la MAG B650 Tomahawk WiFi ofrece VRM de 14 fases para estabilidad en overclocking. WiFi 6E, 2.5G LAN, 4 slots DDR5 hasta 128GB, 2× M.2 PCIe 5.0 y USB 3.2 Gen 2×2. Iluminación ARGB Mystic Light sincronizable con el ecosistema.',
   773109, 19, 'MOBO-MSI-B650TWF-AM5', 'ACTIVE', NOW(), NOW()),

  (p_ddr5_32, seller_tc, cat_ram,
   'RAM DDR5 32GB (2×16GB) 6000MHz CL30 Corsair Vengeance',
   'ram-ddr5-32gb-6000mhz-corsair-vengeance',
   'Corsair',
   'Kit de memoria DDR5 de alto rendimiento para plataformas AM5 e Intel 12/13/14 Gen. 32GB en configuración dual channel, 6000 MHz con latencias CL30. Disipador de aluminio de perfil bajo compatible con coolers voluminosos. Compatible con XMP 3.0 y EXPO para activación automática a velocidad máxima.',
   864706, 19, 'RAM-COR-DDR5-32G6K', 'ACTIVE', NOW(), NOW()),

  (p_ddr4_16, seller_tc, cat_ram,
   'RAM DDR4 16GB (2×8GB) 3200MHz CL16 Kingston Fury Beast',
   'ram-ddr4-16gb-3200mhz-kingston-fury',
   'Kingston',
   'Memoria DDR4 Kingston Fury Beast en kit de 16GB (2×8GB) a 3200 MHz con latencia CL16. Diseño plug-and-play sin necesidad de activar XMP en placas compatibles. Disipador térmico de bajo perfil (34.1mm) ideal para sistemas con cooler de torre grande. Sin iluminación RGB para look minimalista.',
   258824, 19, 'RAM-KIN-DDR4-16G32', 'ACTIVE', NOW(), NOW()),

  (p_990pro, seller_tc, cat_comp,
   'SSD NVMe M.2 1TB PCIe Gen4 Samsung 990 Pro',
   'ssd-nvme-m2-1tb-samsung-990-pro',
   'Samsung',
   'El Samsung 990 Pro es el SSD NVMe más rápido de Samsung con velocidades de lectura secuencial de hasta 7,450 MB/s y escritura de 6,900 MB/s. Formfactor M.2 2280 con interfaz PCIe 4.0 ×4 NVMe 2.0. Incluye disipador de aluminio opcional. Compatible con PS5 (ranura de expansión). 5 años de garantía.',
   388235, 19, 'SSD-SAM-990PRO-1TB', 'ACTIVE', NOW(), NOW()),

  (p_seasonic, seller_tc, cat_comp,
   'Fuente 850W 80 Plus Gold Modular Seasonic Focus GX-850',
   'fuente-850w-gold-modular-seasonic-gx850',
   'Seasonic',
   'La Seasonic Focus GX-850 ofrece 850W certificados 80 Plus Gold con eficiencia del 92% a carga típica. Diseño completamente modular para cableado limpio. Ventilador semi-pasivo que se activa solo cuando es necesario. Protecciones OVP, UVP, OPP, OTP y SCP. Garantía de 10 años.',
   362605, 19, 'PSU-SEA-GX850-GOLD', 'ACTIVE', NOW(), NOW()),

  (p_darkrock, seller_tc, cat_comp,
   'Cooler CPU Torre Dark Rock Pro 4 be quiet! 250W TDP',
   'cooler-cpu-dark-rock-pro-4-be-quiet',
   'be quiet!',
   'El Dark Rock Pro 4 es un cooler de CPU dual-tower de alta gama con dos ventiladores be quiet! Silent Wings 3 de 120mm y 135mm. Disipación de hasta 250W TDP. Compatible con Intel LGA 1700/1200/115x y AMD AM5/AM4. Acabado negro Premium. Nivel de ruido ultra bajo de 24.3 dB(A).',
   339706, 19, 'COOL-BEQ-DRP4-250W', 'ACTIVE', NOW(), NOW());

  -- ────────────────────────────────────────────────────────
  -- 3. PRODUCTOS — PC Master Race CO (monitores, gabinetes)
  -- ────────────────────────────────────────────────────────
  INSERT INTO products (id, seller_id, category_id, name, slug, brand, description, base_price, iva_percent, sku, status, created_at, updated_at) VALUES
  (p_lg4k, seller_pm, cat_mon,
   'Monitor 4K 27" IPS 144Hz LG 27GR95QE-B OLED Gaming',
   'monitor-4k-27-144hz-lg-oled-gaming',
   'LG',
   'El LG 27GR95QE-B es un monitor OLED de 27" con resolución 4K UHD y tasa de refresco de 144Hz para una experiencia visual sin compromisos. Tiempo de respuesta de 0.1ms, DCI-P3 98.5%, HDR10 y Dolby Vision. Soporte G-Sync Compatible y FreeSync Premium. Perfect Black con contraste infinito.',
   2117647, 19, 'MON-LG-27OLED-4K144', 'ACTIVE', NOW(), NOW()),

  (p_mon144, seller_pm, cat_mon,
   'Monitor Gaming 27" IPS 165Hz 1ms QHD MSI G274QPF',
   'monitor-27-ips-165hz-qhd-msi-g274',
   'MSI',
   'Monitor gaming de 27" con panel IPS QHD (2560×1440) y frecuencia de refresco de 165Hz. Tiempo de respuesta GTG de 1ms con tecnología Rapid IPS. Cubierta antireflejo, ángulos de visión de 178°. FreeSync Premium y G-Sync Compatible. Hub USB-A 3.0 ×2 integrado. Soporte ajustable en altura y giro.',
   1109244, 19, 'MON-MSI-G274QPF-165', 'ACTIVE', NOW(), NOW()),

  (p_nzxt, seller_pm, cat_comp,
   'Gabinete Mid-Tower NZXT H510 Flow Vidrio Templado ATX',
   'gabinete-nzxt-h510-flow-vidrio-templado',
   'NZXT',
   'El NZXT H510 Flow combina el diseño icónico de la serie H con un panel frontal de malla para máximo flujo de aire. Compatible con hasta 3 ventiladores de 120mm en frontal, 140mm en superior y 120mm en trasero. Panel lateral de vidrio templado para mostrar tus componentes. Cable management guiado trasero.',
   518908, 19, 'CASE-NZT-H510FL-ATX', 'ACTIVE', NOW(), NOW()),

  (p_h150i, seller_pm, cat_comp,
   'Cooler Líquido 360mm Corsair iCUE H150i Elite Capellix',
   'cooler-liquido-360mm-corsair-h150i',
   'Corsair',
   'La Corsair iCUE H150i Elite Capellix es una refrigeración líquida AIO de 360mm con bomba de alta presión y tres ventiladores LL120 RGB de 120mm. LEDs Capellix de 204 puntos de luz ultrabrillantes. Control total desde iCUE. Compatible con AM5/AM4, LGA 1700/1200/115x. TDP hasta 250W.',
   693277, 19, 'AIO-COR-H150I-360R', 'ACTIVE', NOW(), NOW()),

  (p_argbled, seller_pm, cat_comp,
   'Kit Iluminación ARGB 2m LED Strip Phanteks DRGB Premium',
   'kit-iluminacion-argb-2m-phanteks-drgb',
   'Phanteks',
   'Transforma tu setup con el kit de tiras LED ARGB de Phanteks. 2 metros de cinta ARGB con 30 LEDs por metro, conector de 5V de 3 pines compatible con cualquier placa base moderna. Efectos de iluminación sincronizables con MSI Mystic Light, ASUS Aura Sync y Gigabyte RGB Fusion. Adhesivo 3M incluido.',
   71849, 19, 'LED-PHA-ARGB-2M30', 'ACTIVE', NOW(), NOW()),

  (p_ssd2tb, seller_pm, cat_comp,
   'SSD Externo 2TB USB-C 1050MB/s Samsung T7 Shield',
   'ssd-externo-2tb-samsung-t7-shield',
   'Samsung',
   'El Samsung T7 Shield es un SSD portátil de 2TB con carcasa resistente a caídas de hasta 3 metros. Velocidades de lectura de 1,050 MB/s y escritura de 1,000 MB/s vía USB 3.2 Gen 2. Incluye cables USB-C a USB-C y USB-C a USB-A. Compatible con PC, Mac, Android, PS4 y Xbox. AES-256 integrado.',
   739496, 19, 'SSD-SAM-T7SH-2TBEXT', 'ACTIVE', NOW(), NOW()),

  (p_hdd4tb, seller_pm, cat_comp,
   'Disco Duro Externo 4TB USB 3.0 Seagate Backup Plus',
   'hdd-externo-4tb-seagate-backup-plus',
   'Seagate',
   'El Seagate Backup Plus Slim de 4TB ofrece gran capacidad de almacenamiento en un factor de forma portátil. Velocidad USB 3.0 compatible con USB 2.0. Incluye 2 años de suscripción a Mylio Photos y 4 meses de Adobe Creative Cloud Photography. Software Seagate Dashboard para backup automatizado.',
   458824, 19, 'HDD-SEA-BP4TB-EXT30', 'ACTIVE', NOW(), NOW()),

  (p_ups, seller_pm, cat_comp,
   'UPS 1500VA 900W AVR LCD APC Back-UPS Pro 1500',
   'ups-1500va-900w-apc-back-ups-pro',
   'APC',
   'El APC Back-UPS Pro 1500 protege tu equipo con 1500VA/900W de potencia, regulación automática de voltaje (AVR) y pantalla LCD de estado. 10 tomas de corriente (8 con batería + 2 solo supresoras). Puerto USB para comunicación con el PC y apagado automático. Batería reemplazable por el usuario.',
   542857, 19, 'UPS-APC-BX1500-PRO', 'ACTIVE', NOW(), NOW()),

  (p_hub, seller_pm, cat_comp,
   'Hub USB-C 10 en 1 100W PD Anker PowerExpand Elite',
   'hub-usb-c-10-en-1-anker-powerexpand',
   'Anker',
   'El Anker PowerExpand Elite 10-in-1 convierte tu puerto USB-C en 10 puertos esenciales: Thunderbolt 3 85W PD, HDMI 4K@60Hz, DisplayPort 4K@60Hz, 2× USB-A 3.0, SD, microSD, 3.5mm audio y Ethernet Gigabit. Construcción en aluminio premium con cable trenzado de 30cm.',
   151261, 19, 'HUB-ANK-PWREXP-10P', 'ACTIVE', NOW(), NOW()),

  (p_qi15w, seller_pm, cat_peri,
   'Cargador Inalámbrico Qi 15W Belkin BoostCharge Pro',
   'cargador-qi-15w-belkin-boostcharge-pro',
   'Belkin',
   'El Belkin BoostCharge Pro carga tu smartphone a 15W con protocolo Qi 2 compatible con iPhone y Android. Indicador LED de carga, protección contra sobrecalentamiento y sobretensión. Incluye adaptador de corriente de 25W. Carga a través de fundas de hasta 3mm de grosor.',
   90756, 19, 'CHG-BLK-BSTCHPRO-15W', 'ACTIVE', NOW(), NOW());

  -- ────────────────────────────────────────────────────────
  -- 4. PRODUCTOS — Gamer Paradise (consolas, juegos)
  -- ────────────────────────────────────────────────────────
  INSERT INTO products (id, seller_id, category_id, name, slug, brand, description, base_price, iva_percent, sku, status, created_at, updated_at) VALUES
  (p_switch, seller_gp, cat_sw,
   'Nintendo Switch OLED Blanco 64GB + Joy-Con Blancos',
   'nintendo-switch-oled-blanco-64gb',
   'Nintendo',
   'La Nintendo Switch (OLED) presenta una pantalla OLED vibrante de 7 pulgadas para jugar en modo portátil. 64GB de almacenamiento interno expandible via microSD. Los Joy-Con se pueden usar en modo TV, de sobremesa o portátil. Base incluida con LAN Ethernet integrado. Altavoz con audio mejorado.',
   1344538, 19, 'CON-NIN-SWITCH-OLED', 'ACTIVE', NOW(), NOW()),

  (p_ds5blco, seller_gp, cat_ps,
   'Control PS5 DualSense Blanco Haptic Feedback + Gatillos',
   'control-ps5-dualsense-blanco',
   'Sony',
   'El DualSense de PlayStation 5 redefine la inmersión con retroalimentación háptica avanzada y gatillos adaptativos que ofrecen resistencia real según la acción del juego. Micrófono integrado, barra de luz LED multicolor, batería recargable via USB-C y panel táctil multitáctil. 5 metros de alcance inalámbrico.',
   287395, 19, 'PAD-SON-DS5-WHT', 'ACTIVE', NOW(), NOW()),

  (p_ds5edge, seller_gp, cat_ps,
   'Control PS5 DualSense Edge Alta Competencia + Case',
   'control-ps5-dualsense-edge-pro',
   'Sony',
   'El DualSense Edge es el control inalámbrico profesional de PlayStation 5. Botones traseros intercambiables, sticks modulares con 3 perfiles de sensibilidad, gatillos de recorrido corto para competición y botones de función personalizables. Incluye estuche de transporte y cables de batería adicionales.',
   1058824, 19, 'PAD-SON-DS5-EDGE', 'ACTIVE', NOW(), NOW()),

  (p_xbox_pad, seller_gp, cat_xbox,
   'Control Xbox Series X Carbon Black + Pila Recargable',
   'control-xbox-series-x-carbon-black',
   'Microsoft',
   'El Control Xbox Series X Carbon Black tiene texturas traseras antideslizantes, D-pad facetado y conectividad Bluetooth para PC, iOS y Android. Botón compartir para capturas directas, puerto USB-C para juego con cable. Compatible con Xbox Series X|S, Xbox One y Windows 10/11.',
   287395, 19, 'PAD-XBOX-SERX-BLK', 'ACTIVE', NOW(), NOW()),

  (p_elden, seller_gp, cat_games,
   'Elden Ring PS5 + DLC Shadow of the Erdtree Bundle',
   'elden-ring-ps5-shadow-erdtree',
   'Bandai Namco',
   'El GOTY de 2022 llega en su edición más completa. Elden Ring para PS5 incluye el DLC Shadow of the Erdtree, la expansión más esperada del juego. Mundo abierto masivo diseñado por Hidetaka Miyazaki y George R.R. Martin. Ray tracing y 60fps estables en PS5. Más de 100 horas de contenido.',
   258824, 19, 'JUG-BN-ERINGP5-DLC', 'ACTIVE', NOW(), NOW()),

  (p_gow, seller_gp, cat_games,
   'God of War Ragnarök PS5 Edición Valhalla',
   'god-of-war-ragnarok-ps5-valhalla',
   'Sony Santa Monica',
   'Kratos y Atreus continúan su épica odisea nórdica en este juego de acción y aventura premiado con múltiples GOTY. La edición Valhalla incluye el DLC gratuito "Valhalla" en disco. Gráficos en 4K y 60fps en PS5 con retroalimentación háptica del DualSense. Más de 40 horas de historia principal.',
   196639, 19, 'JUG-SON-GOWRAG-VAL', 'ACTIVE', NOW(), NOW()),

  (p_dxracer, seller_gp, cat_silla,
   'Silla Gamer Ergonómica DXRacer Formula Series OH/FE08/N',
   'silla-gamer-dxracer-formula-series',
   'DXRacer',
   'La DXRacer Formula Series es la silla gaming de referencia para sesiones largas. Espuma de alta densidad de 60kg/m3, altura ajustable con pistón clase 3 certificado SGS, reposabrazos 4D, reclinación hasta 135° con función mecedora. Incluye almohada lumbar y cojín cervical. Capacidad hasta 80kg.',
   718487, 19, 'SLL-DXR-FE08N-BLK', 'ACTIVE', NOW(), NOW()),

  (p_ergotron, seller_gp, cat_peri,
   'Soporte Monitor Articulado VESA Ergotron LX Desk Arm',
   'soporte-monitor-ergotron-lx-desk-arm',
   'Ergotron',
   'El Ergotron LX es el soporte de escritorio más vendido del mundo. Soporta monitores de 21" a 34" hasta 11.3kg con VESA 75×75 o 100×100. Gestión de cables integrada, movimiento Constant Force para reposicionamiento sin esfuerzo. Inclinación ±90°, giro 360°, altura ajustable 46cm. Garantía limitada de vida.',
   242017, 19, 'SOP-ERG-LX-DESK34', 'ACTIVE', NOW(), NOW()),

  (p_razerbase, seller_gp, cat_peri,
   'Soporte Headset RGB Razer Base Station V2 Chroma',
   'soporte-headset-razer-base-station-v2-chroma',
   'Razer',
   'El Razer Base Station V2 Chroma es el soporte de auriculares gaming más premium del mercado. 3 puertos USB-A 3.0 integrados, 1 puerto USB-C 3.0 y conector de audio/micrófono de 3.5mm. Iluminación Razer Chroma RGB de 16.8 millones de colores con más de 75 efectos. Soporte universal, antideslizante.',
   124790, 19, 'SOP-RZR-BASEV2-CHR', 'ACTIVE', NOW(), NOW()),

  (p_mpad_xxl, seller_gp, cat_mouse,
   'Mousepad XXL RGB 900×400mm Borde Cosido SteelSeries QcK',
   'mousepad-xxl-rgb-900x400-steelseries-qck',
   'SteelSeries',
   'El SteelSeries QcK XXL RGB cubre todo tu escritorio (900×400×4mm) con un borde cosido de alta durabilidad. Superficie de tela micro-tejida optimizada para sensores ópticos y láser. 12 zonas de iluminación RGB configurables desde SteelSeries Engine. Base antideslizante de caucho natural. Resistente a derrames.',
   75000, 19, 'MPD-STL-QCKXXL-RGB', 'ACTIVE', NOW(), NOW());

  -- ────────────────────────────────────────────────────────
  -- 5. PRODUCTOS — NextGen Gaming (streaming, audio)
  -- ────────────────────────────────────────────────────────
  INSERT INTO products (id, seller_id, category_id, name, slug, brand, description, base_price, iva_percent, sku, status, created_at, updated_at) VALUES
  (p_brio, seller_ng, cat_stream,
   'Webcam 4K 60fps Logitech Brio Ultra HD + Micrófonos',
   'webcam-4k-logitech-brio-ultra-hd',
   'Logitech',
   'La Logitech Brio es la webcam de referencia para streamers y videoconferencias profesionales. 4K Ultra HD a 30fps o 1080p a 60fps con HDR automático. Micrófonos duales omnidireccionales, campo de visión de 65/78/90° ajustable. Windows Hello facial recognition, USB-C. Compatible con Zoom, Teams y OBS.',
   571429, 19, 'WEB-LOG-BRIO-4K60', 'ACTIVE', NOW(), NOW()),

  (p_yeti, seller_ng, cat_stream,
   'Micrófono USB Blue Yeti Cárdioide + Soporte Anti-vibración',
   'microfono-usb-blue-yeti-cardiode',
   'Blue',
   'El Blue Yeti es el micrófono USB más vendido en el mundo. Cuatro patrones polares (cárdioide, bidireccionam, omnidireccional, estéreo) seleccionables en hardware. Control de ganancia, silenciado instantáneo y monitoreo de audio sin latencia. Resolución de 16 bits/48kHz. Plug-and-play sin drivers.',
   256000, 19, 'MIC-BLU-YETI-USB4P', 'ACTIVE', NOW(), NOW()),

  (p_streamdk, seller_ng, cat_stream,
   'Elgato Stream Deck MK.2 15 Teclas LCD Personalizables',
   'elgato-stream-deck-mk2-15-teclas',
   'Elgato',
   'El Stream Deck MK.2 pone 15 teclas LCD personalizables al alcance de tu mano para controlar tu stream, apps, atajos y mucho más. Integración nativa con OBS, Streamlabs, Twitch, YouTube, Spotify y más de 50 aplicaciones. Teclas con imágenes a color individuales, conexión USB-A. Software gratuito.',
   622689, 19, 'STR-ELG-SDMK2-15K', 'ACTIVE', NOW(), NOW()),

  (p_elgato4k, seller_ng, cat_stream,
   'Capturadora Elgato 4K60 Pro MK.2 HDMI PCIe Gaming',
   'elgato-4k60-pro-mk2-capturadora',
   'Elgato',
   'La Elgato 4K60 Pro MK.2 captura video 4K a 60fps desde consolas y PCs directamente a tu PC a través del slot PCIe x4. Sin latencia en passthrough 4K HDR10. Audio multicanal, compatible con PS5, Xbox Series X y PC. Requiere Elgato 4K Capture Utility o soluciones de terceros como OBS.',
   438655, 19, 'CAP-ELG-4K60PROM2', 'ACTIVE', NOW(), NOW()),

  (p_ringlt, seller_ng, cat_stream,
   'Ring Light LED 18" Neewer 55W con Tripié y Soporte Phone',
   'ring-light-led-18-neewer-55w-tripie',
   'Neewer',
   'El ring light de 18" de Neewer con 55W de potencia ofrece iluminación suave y uniforme para streaming, YouTube y videollamadas. Temperatura de color ajustable 3200K-5600K, brillo regulable al 1%. Tripié de 200cm con cabezal giratorio, soporte para smartphone y anillo difusor. Control remoto incluido.',
   203361, 19, 'LIG-NEW-RL18-55W', 'ACTIVE', NOW(), NOW()),

  (p_canonm50, seller_ng, cat_stream,
   'Cámara Canon EOS M50 Mark II 24MP 4K + Lente 15-45mm',
   'camara-canon-eos-m50-mark2-24mp',
   'Canon',
   'La Canon EOS M50 Mark II es la cámara mirrorless ideal para creadores de contenido. Sensor CMOS de 24.1 MP, video 4K y Full HD a 120fps. LCD táctil giratorio de 3", Eye Detect AF para mantener el foco en personas. Transmisión en vivo a YouTube, 5 fps ráfaga. Kit incluye lente EF-M 15-45mm IS STM.',
   2394958, 19, 'CAM-CAN-M50MK2-4K', 'ACTIVE', NOW(), NOW()),

  (p_asusrog, seller_ng, cat_peri,
   'Router WiFi 6 AX6000 Gaming ASUS ROG Rapture GT-AX6000',
   'router-wifi6-asus-rog-rapture-gt-ax6000',
   'ASUS',
   'El ASUS ROG Rapture GT-AX6000 es el router gaming definitivo con WiFi 6 (802.11ax) de doble banda 6000 Mbps. Procesador quad-core a 2.0GHz, 8 antenas de alto rendimiento, puerto WAN 2.5G y 4× LAN 2.5G. Game Acceleration, Adaptive QoS, AiMesh 2.0 y seguridad ASUS AiProtection Pro gratuita.',
   710504, 19, 'RTR-ASS-GT6000-W6', 'ACTIVE', NOW(), NOW()),

  (p_z623, seller_ng, cat_peri,
   'Parlantes 2.1 RGB 60W RMS Logitech Z623 THX Certified',
   'parlantes-21-logitech-z623-thx-60w',
   'Logitech',
   'Los Logitech Z623 son el sistema de altavoces 2.1 con certificación THX para audio de referencia cinematográfico. 200W de potencia pico (60W RMS), subwoofer de 8" para bajo potente, entradas Jack 3.5mm dual, RCA estéreo y entrada de audio digital. Control de volumen y bajos con perilla frontal.',
   285714, 19, 'SPK-LOG-Z623-THX21', 'ACTIVE', NOW(), NOW()),

  (p_hdmi21, seller_ng, cat_peri,
   'Cable HDMI 2.1 8K 4K 120Hz 3m Certificado VESA',
   'cable-hdmi-21-8k-120hz-3m-certificado',
   'Ugreen',
   'Cable HDMI 2.1 ultra premium certificado con soporte para resoluciones hasta 8K@60Hz y 4K@120Hz. Ancho de banda de 48Gbps, compatible con eARC, VRR, ALLM y HDR10+. Longitud de 3 metros con blindaje de aluminio de 4 capas. Compatible con PS5, Xbox Series X, Samsung Neo QLED y monitores 4K gaming.',
   39706, 19, 'CBL-UGR-HDMI21-3M', 'ACTIVE', NOW(), NOW()),

  (p_sony_wh, seller_ng, cat_head,
   'Audífonos Bluetooth 5.3 ANC Sony WH-1000XM5 Premium',
   'audifonos-bluetooth-sony-wh-1000xm5',
   'Sony',
   'Los Sony WH-1000XM5 son los mejores audífonos con cancelación activa de ruido del mundo. 8 micrófonos con chip QN1 y V1 para ANC líder en la industria. 30 horas de batería (sin ANC: 40h), carga rápida 3 minutos = 3 horas. Codecs LDAC, AAC y SBC. Altavoces de 30mm con respuesta desde 4Hz.',
   462605, 19, 'AUD-SON-WH1000X5', 'ACTIVE', NOW(), NOW());

  -- ────────────────────────────────────────────────────────
  -- 6. PRODUCTOS — Perifericos Pro (mouses, teclados)
  -- ────────────────────────────────────────────────────────
  INSERT INTO products (id, seller_id, category_id, name, slug, brand, description, base_price, iva_percent, sku, status, created_at, updated_at) VALUES
  (p_gpro, seller_pp, cat_mouse,
   'Mouse Gaming Logitech G Pro X Superlight 2 25600DPI',
   'mouse-logitech-g-pro-x-superlight-2',
   'Logitech',
   'El Logitech G Pro X Superlight 2 redefine el mouse gaming inalámbrico con tan solo 60g de peso. Sensor HERO 2 de 25,600 DPI sin suavizado, filtrado ni aceleración. Tecnología LIGHTSPEED con latencia de 1ms, batería para 95 horas. Pies PTFE de 100% para deslizamiento perfecto. Rediseñado para pros.',
   295798, 19, 'MUS-LOG-GPXSL2-60G', 'ACTIVE', NOW(), NOW()),

  (p_g502, seller_pp, cat_mouse,
   'Mouse Gaming Logitech G502 X Plus RGB LIGHTSPEED 89G',
   'mouse-logitech-g502-x-plus-rgb',
   'Logitech',
   'El G502 X Plus combina el diseño ergonómico favorito de los gamers con tecnología inalámbrica LIGHTSPEED y RGB LIGHTFORCE. 13 botones programables, rueda de desplazamiento LIGHTSPEED de doble modo, pesos sintonizables de 16g. Sensor HERO 25K con seguimiento a 25,600 DPI. 130 horas de batería sin RGB.',
   143697, 19, 'MUS-LOG-G502XPLUS', 'ACTIVE', NOW(), NOW()),

  (p_mxvert, seller_pp, cat_mouse,
   'Mouse Ergonómico Vertical Logitech MX Vertical Inalámbrico',
   'mouse-ergonomico-vertical-logitech-mx',
   'Logitech',
   'El Logitech MX Vertical reduce la tensión muscular del antebrazo en un 10% con su diseño ergonómico a 57°. Sensor óptico de 4000 DPI ajustable, 4 botones programables. Conexión Bluetooth o USB 3.0. Compatible con Easy-Switch para cambiar entre 3 dispositivos. Carga rápida USB-C (1 minuto = 3 horas de uso).',
   152521, 19, 'MUS-LOG-MXVERT-V', 'ACTIVE', NOW(), NOW()),

  (p_hyper, seller_pp, cat_key,
   'Teclado Mecánico Gaming RGB HyperX Alloy Origins Switch Red',
   'teclado-mecanico-hyperx-alloy-origins-red',
   'HyperX',
   'El HyperX Alloy Origins cuenta con switches mecánicos HyperX Red lineales de 45g de actuación para gaming de alta velocidad. Iluminación RGB per-key personalizable, marco de aluminio completo para durabilidad extrema. Cable USB-C desmontable, función NKRO completa. Software HyperX NGENUITY para macros.',
   294118, 19, 'KEY-HPX-ALLORIGINS-R', 'ACTIVE', NOW(), NOW()),

  (p_k6, seller_pp, cat_key,
   'Teclado Mecánico 65% Hot-Swap RGB Keychron K6 Pro',
   'teclado-mecanico-65-keychron-k6-pro',
   'Keychron',
   'El Keychron K6 Pro es el teclado mecánico personalizable por excelencia. Factor 65% con hotswap PCB para cambiar switches sin soldar (compatible con MX y Kailh). Tres capas de materiales amortiguadores para sonido premium. Bluetooth 5.1 multidevice, RGB south-facing, batería de 4000mAh. Mac y Windows.',
   383193, 19, 'KEY-KCH-K6PRO-65HS', 'ACTIVE', NOW(), NOW()),

  (p_ducky, seller_pp, cat_key,
   'Teclado Mecánico TKL Ducky One 3 Daybreak Switch Red',
   'teclado-mecanico-ducky-one-3-daybreak-tkl',
   'Ducky',
   'El Ducky One 3 Daybreak es un teclado mecánico TKL (sin numpad) con diseño minimalista en tonos azul-gris-morado. PCB hot-swap para cambio de switches sin soldar, compatibles con Cherry MX y MX-style. Foam case-foam y switch-foam para sonido amortiguado. Switches Cherry MX Red lineales.',
   282353, 19, 'KEY-DUC-ONE3DB-TKL', 'ACTIVE', NOW(), NOW()),

  (p_strider, seller_pp, cat_mouse,
   'Mousepad Duro XL Control Speed Razer Strider 940×410mm',
   'mousepad-duro-xl-razer-strider-940',
   'Razer',
   'El Razer Strider es el primer mousepad híbrido de Razer: tela suave por arriba para deslizamiento preciso y base de goma texturizada antideslizante. Tamaño XL de 940×410×3mm para cubrir todo el escritorio. Borde cosido para durabilidad extrema. Superficie impermeable, resistente a derrames y fácil de limpiar.',
   87815, 19, 'MPD-RZR-STRDXL-940', 'ACTIVE', NOW(), NOW()),

  (p_artisan, seller_pp, cat_mouse,
   'Mousepad Control Speed Artisan Zero Soft XL 490×420mm',
   'mousepad-artisan-zero-soft-xl',
   'Artisan',
   'Artisan es el fabricante japonés de mousepads premium por excelencia. El Zero Soft XL (490×420×3mm) ofrece la superficie de control más fina y precisa del mundo. Tela de alta densidad sin textura perceptible. Base antideslizante nano-suction adherente. Ideal para sensibilidades bajas en FPS.',
   92437, 19, 'MPD-ART-ZEROSXL-JPN', 'ACTIVE', NOW(), NOW()),

  (p_arctis, seller_pp, cat_head,
   'Headset Gaming 7.1 Surround SteelSeries Arctis Nova Pro',
   'headset-steelseries-arctis-nova-pro-71',
   'SteelSeries',
   'El SteelSeries Arctis Nova Pro es el headset gaming premium para PC y PlayStation. Audio Hi-Fi con drivers de neodimio de 40mm y respuesta de frecuencia de 10-40,000 Hz. Cancelación activa de ruido de alta fidelidad, modo Transparency, DAC externo con pantalla OLED. Dos baterías intercambiables para uso infinito.',
   370588, 19, 'HED-STL-NOVAPRO-71', 'ACTIVE', NOW(), NOW()),

  (p_hs80, seller_pp, cat_head,
   'Headset Inalámbrico 2.4GHz 65h Corsair HS80 Max RGB',
   'headset-inalambrico-corsair-hs80-max',
   'Corsair',
   'El Corsair HS80 Max ofrece audio inalámbrico 2.4GHz de alta fidelidad con hasta 65 horas de batería. Drivers personalizados de 50mm con membrana de fibra de carbono, micrófono omnidireccional desmontable certificado Discord. Sonido espacial Dolby Audio 7.1 y DTS Headphone:X vía USB. RGB CAPELLIX integrado.',
   491597, 19, 'HED-COR-HS80MAX-24G', 'ACTIVE', NOW(), NOW());

  -- ────────────────────────────────────────────────────────
  -- 7. IMÁGENES DE PRODUCTOS (2 por producto)
  -- ────────────────────────────────────────────────────────
  INSERT INTO product_images (id, product_id, url, alt_text, is_primary, sort_order, created_at) VALUES
  -- TechStore Colombia
  (gen_random_uuid(), p_rtx4070, img_gpu,  'RTX 4070 Super GPU front view', true,  0, NOW()),
  (gen_random_uuid(), p_rtx4070, img_gpu2, 'RTX 4070 Super in PC build',    false, 1, NOW()),
  (gen_random_uuid(), p_rx7800,  img_gpu2, 'RX 7800 XT GPU front view',     true,  0, NOW()),
  (gen_random_uuid(), p_rx7800,  img_gpu,  'RX 7800 XT in system',          false, 1, NOW()),
  (gen_random_uuid(), p_i7_14k,  img_cpu,  'Intel Core i7-14700K processor', true, 0, NOW()),
  (gen_random_uuid(), p_i7_14k,  img_mobo, 'i7-14700K on motherboard',      false, 1, NOW()),
  (gen_random_uuid(), p_r7_7700x,img_cpu,  'AMD Ryzen 7 7700X processor',   true,  0, NOW()),
  (gen_random_uuid(), p_r7_7700x,img_mobo, 'Ryzen 7 7700X setup',           false, 1, NOW()),
  (gen_random_uuid(), p_b650,    img_mobo, 'MSI MAG B650 Tomahawk top view', true, 0, NOW()),
  (gen_random_uuid(), p_b650,    img_setup,'MSI B650 in gaming build',       false, 1, NOW()),
  (gen_random_uuid(), p_ddr5_32, img_ram,  'Corsair Vengeance DDR5 sticks',  true, 0, NOW()),
  (gen_random_uuid(), p_ddr5_32, img_mobo, 'DDR5 installed on motherboard',  false,1, NOW()),
  (gen_random_uuid(), p_ddr4_16, img_ram,  'Kingston Fury Beast DDR4 kit',   true, 0, NOW()),
  (gen_random_uuid(), p_ddr4_16, img_setup,'Kingston Fury in gaming rig',    false,1, NOW()),
  (gen_random_uuid(), p_990pro,  img_ssd,  'Samsung 990 Pro NVMe M.2',       true, 0, NOW()),
  (gen_random_uuid(), p_990pro,  img_mobo, '990 Pro installed in M.2 slot',  false,1, NOW()),
  (gen_random_uuid(), p_seasonic,img_gpu2, 'Seasonic Focus GX-850 PSU',      true, 0, NOW()),
  (gen_random_uuid(), p_seasonic,img_case_,'Seasonic in modular build',       false,1, NOW()),
  (gen_random_uuid(), p_darkrock,img_cpu,  'be quiet! Dark Rock Pro 4',      true, 0, NOW()),
  (gen_random_uuid(), p_darkrock,img_setup,'Dark Rock Pro 4 in case',        false,1, NOW()),
  -- PC Master Race CO
  (gen_random_uuid(), p_lg4k,   img_mon,  'LG OLED 27 4K gaming monitor',   true, 0, NOW()),
  (gen_random_uuid(), p_lg4k,   img_setup,'LG 4K OLED desk setup',          false, 1, NOW()),
  (gen_random_uuid(), p_mon144, img_mon,  'MSI G274 27 QHD 165Hz monitor',  true, 0, NOW()),
  (gen_random_uuid(), p_mon144, img_setup,'MSI monitor gaming setup',        false, 1, NOW()),
  (gen_random_uuid(), p_nzxt,   img_case_,'NZXT H510 Flow black case',       true, 0, NOW()),
  (gen_random_uuid(), p_nzxt,   img_setup,'NZXT H510 build interior',        false, 1, NOW()),
  (gen_random_uuid(), p_h150i,  img_cpu,  'Corsair H150i 360mm AIO cooler',  true, 0, NOW()),
  (gen_random_uuid(), p_h150i,  img_setup,'H150i installed in case',         false, 1, NOW()),
  (gen_random_uuid(), p_argbled,img_light,'Phanteks ARGB LED strips active', true, 0, NOW()),
  (gen_random_uuid(), p_argbled,img_setup,'ARGB setup with strips',          false, 1, NOW()),
  (gen_random_uuid(), p_ssd2tb, img_ssd,  'Samsung T7 Shield 2TB portable',  true, 0, NOW()),
  (gen_random_uuid(), p_ssd2tb, img_cable,'T7 Shield USB-C connection',      false, 1, NOW()),
  (gen_random_uuid(), p_hdd4tb, img_ssd,  'Seagate Backup Plus 4TB drive',   true, 0, NOW()),
  (gen_random_uuid(), p_hdd4tb, img_cable,'Seagate with USB 3.0 cable',      false, 1, NOW()),
  (gen_random_uuid(), p_ups,    img_ups,  'APC Back-UPS Pro 1500 front',     true, 0, NOW()),
  (gen_random_uuid(), p_ups,    img_cable,'APC UPS back connections',        false, 1, NOW()),
  (gen_random_uuid(), p_hub,    img_cable,'Anker PowerExpand 10-in-1 hub',   true, 0, NOW()),
  (gen_random_uuid(), p_hub,    img_ssd,  'Anker hub with laptop',           false, 1, NOW()),
  (gen_random_uuid(), p_qi15w,  img_cable,'Belkin BoostCharge Pro Qi pad',   true, 0, NOW()),
  (gen_random_uuid(), p_qi15w,  img_setup,'Belkin charger on desk',          false, 1, NOW()),
  -- Gamer Paradise
  (gen_random_uuid(), p_switch, img_nswitch,'Nintendo Switch OLED white',    true, 0, NOW()),
  (gen_random_uuid(), p_switch, img_game,  'Switch OLED gaming lifestyle',   false, 1, NOW()),
  (gen_random_uuid(), p_ds5blco,img_ctrl,  'PS5 DualSense White front',      true, 0, NOW()),
  (gen_random_uuid(), p_ds5blco,img_ctrl2, 'DualSense White in hand',        false, 1, NOW()),
  (gen_random_uuid(), p_ds5edge,img_ctrl,  'PS5 DualSense Edge front',       true, 0, NOW()),
  (gen_random_uuid(), p_ds5edge,img_ctrl2, 'DualSense Edge with case',       false, 1, NOW()),
  (gen_random_uuid(), p_xbox_pad,img_ctrl2,'Xbox Series X pad Carbon Black', true, 0, NOW()),
  (gen_random_uuid(), p_xbox_pad,img_ctrl, 'Xbox pad on surface',            false, 1, NOW()),
  (gen_random_uuid(), p_elden,  img_game, 'Elden Ring PS5 box art',           true, 0, NOW()),
  (gen_random_uuid(), p_elden,  img_ctrl, 'Elden Ring gameplay screenshot',  false, 1, NOW()),
  (gen_random_uuid(), p_gow,    img_game, 'God of War Ragnarök PS5 box',      true, 0, NOW()),
  (gen_random_uuid(), p_gow,    img_ctrl, 'God of War gameplay',             false, 1, NOW()),
  (gen_random_uuid(), p_dxracer,img_chair,'DXRacer Formula Series black',    true, 0, NOW()),
  (gen_random_uuid(), p_dxracer,img_setup,'DXRacer at gaming desk',          false, 1, NOW()),
  (gen_random_uuid(), p_ergotron,img_mon, 'Ergotron LX arm with monitor',    true, 0, NOW()),
  (gen_random_uuid(), p_ergotron,img_setup,'Ergotron LX desk setup',         false, 1, NOW()),
  (gen_random_uuid(), p_razerbase,img_headset,'Razer Base Station V2 Chroma',true, 0, NOW()),
  (gen_random_uuid(), p_razerbase,img_setup,'Razer Base Station on desk',    false, 1, NOW()),
  (gen_random_uuid(), p_mpad_xxl,img_mpad,'SteelSeries QcK XXL RGB mat',     true, 0, NOW()),
  (gen_random_uuid(), p_mpad_xxl,img_setup,'XXL mousepad on gaming desk',    false, 1, NOW()),
  -- NextGen Gaming
  (gen_random_uuid(), p_brio,   img_webcam,'Logitech Brio 4K webcam',        true, 0, NOW()),
  (gen_random_uuid(), p_brio,   img_stream,'Brio on monitor in studio',      false, 1, NOW()),
  (gen_random_uuid(), p_yeti,   img_mic,  'Blue Yeti USB microphone',         true, 0, NOW()),
  (gen_random_uuid(), p_yeti,   img_stream,'Blue Yeti streaming setup',      false, 1, NOW()),
  (gen_random_uuid(), p_streamdk,img_stream,'Elgato Stream Deck MK.2',       true, 0, NOW()),
  (gen_random_uuid(), p_streamdk,img_setup,'Stream Deck on streaming desk',  false, 1, NOW()),
  (gen_random_uuid(), p_elgato4k,img_stream,'Elgato 4K60 Pro MK.2 PCIe',    true, 0, NOW()),
  (gen_random_uuid(), p_elgato4k,img_setup,'4K60 Pro installed in PC',       false, 1, NOW()),
  (gen_random_uuid(), p_ringlt, img_light,'Neewer 18in ring light active',   true, 0, NOW()),
  (gen_random_uuid(), p_ringlt, img_stream,'Ring light in studio setup',     false, 1, NOW()),
  (gen_random_uuid(), p_canonm50,img_cam, 'Canon EOS M50 Mark II front',     true, 0, NOW()),
  (gen_random_uuid(), p_canonm50,img_webcam,'Canon M50 on tripod',           false, 1, NOW()),
  (gen_random_uuid(), p_asusrog, img_router,'ASUS ROG Rapture GT-AX6000',    true, 0, NOW()),
  (gen_random_uuid(), p_asusrog, img_cable,'ASUS ROG router antenna detail', false, 1, NOW()),
  (gen_random_uuid(), p_z623,   img_speaker,'Logitech Z623 2.1 speakers',    true, 0, NOW()),
  (gen_random_uuid(), p_z623,   img_setup,'Z623 on gaming desk setup',       false, 1, NOW()),
  (gen_random_uuid(), p_hdmi21, img_cable,'Ugreen HDMI 2.1 cable 3m',        true, 0, NOW()),
  (gen_random_uuid(), p_hdmi21, img_setup,'HDMI 2.1 connected to monitor',   false, 1, NOW()),
  (gen_random_uuid(), p_sony_wh,img_headset,'Sony WH-1000XM5 white',         true, 0, NOW()),
  (gen_random_uuid(), p_sony_wh,img_headset2,'WH-1000XM5 lifestyle shot',    false, 1, NOW()),
  -- Perifericos Pro
  (gen_random_uuid(), p_gpro,  img_mouse, 'Logitech G Pro X Superlight 2',   true, 0, NOW()),
  (gen_random_uuid(), p_gpro,  img_setup, 'G Pro X Superlight 2 on pad',     false, 1, NOW()),
  (gen_random_uuid(), p_g502,  img_mouse, 'Logitech G502 X Plus RGB',        true, 0, NOW()),
  (gen_random_uuid(), p_g502,  img_setup, 'G502 X Plus on gaming setup',     false, 1, NOW()),
  (gen_random_uuid(), p_mxvert,img_mouse, 'Logitech MX Vertical ergonomic',  true, 0, NOW()),
  (gen_random_uuid(), p_mxvert,img_setup, 'MX Vertical on work desk',        false, 1, NOW()),
  (gen_random_uuid(), p_hyper, img_key,   'HyperX Alloy Origins RGB red',    true, 0, NOW()),
  (gen_random_uuid(), p_hyper, img_key2,  'Alloy Origins close-up switches', false, 1, NOW()),
  (gen_random_uuid(), p_k6,    img_key2,  'Keychron K6 Pro 65 percent',      true, 0, NOW()),
  (gen_random_uuid(), p_k6,    img_key,   'K6 Pro RGB south-facing',         false, 1, NOW()),
  (gen_random_uuid(), p_ducky, img_key,   'Ducky One 3 Daybreak TKL',        true, 0, NOW()),
  (gen_random_uuid(), p_ducky, img_key2,  'Ducky One 3 switches close-up',   false, 1, NOW()),
  (gen_random_uuid(), p_strider,img_mpad, 'Razer Strider XL hybrid pad',     true, 0, NOW()),
  (gen_random_uuid(), p_strider,img_mouse,'Strider XL with mouse on top',    false, 1, NOW()),
  (gen_random_uuid(), p_artisan,img_mpad, 'Artisan Zero Soft XL Japanese',   true, 0, NOW()),
  (gen_random_uuid(), p_artisan,img_mouse,'Artisan Zero with gaming mouse',  false, 1, NOW()),
  (gen_random_uuid(), p_arctis, img_headset,'SteelSeries Arctis Nova Pro',   true, 0, NOW()),
  (gen_random_uuid(), p_arctis, img_headset2,'Arctis Nova Pro DAC detail',   false, 1, NOW()),
  (gen_random_uuid(), p_hs80,  img_headset2,'Corsair HS80 Max RGB wireless', true, 0, NOW()),
  (gen_random_uuid(), p_hs80,  img_headset,'HS80 Max dongle and cable',      false, 1, NOW());

  -- ────────────────────────────────────────────────────────
  -- 8. INVENTARIO (stock realista por producto)
  -- ────────────────────────────────────────────────────────
  INSERT INTO inventory (id, product_id, physical_stock, reserved_stock, created_at, updated_at) VALUES
  (gen_random_uuid(), p_rtx4070,  8,  0, NOW(), NOW()),
  (gen_random_uuid(), p_rx7800,  10,  0, NOW(), NOW()),
  (gen_random_uuid(), p_i7_14k,  12,  0, NOW(), NOW()),
  (gen_random_uuid(), p_r7_7700x,15,  0, NOW(), NOW()),
  (gen_random_uuid(), p_b650,    18,  0, NOW(), NOW()),
  (gen_random_uuid(), p_ddr5_32, 25,  0, NOW(), NOW()),
  (gen_random_uuid(), p_ddr4_16, 40,  0, NOW(), NOW()),
  (gen_random_uuid(), p_990pro,  30,  0, NOW(), NOW()),
  (gen_random_uuid(), p_seasonic, 20, 0, NOW(), NOW()),
  (gen_random_uuid(), p_darkrock, 14, 0, NOW(), NOW()),
  (gen_random_uuid(), p_lg4k,     5, 0, NOW(), NOW()),
  (gen_random_uuid(), p_mon144,  12, 0, NOW(), NOW()),
  (gen_random_uuid(), p_nzxt,    20, 0, NOW(), NOW()),
  (gen_random_uuid(), p_h150i,   16, 0, NOW(), NOW()),
  (gen_random_uuid(), p_argbled, 50, 0, NOW(), NOW()),
  (gen_random_uuid(), p_ssd2tb,  18, 0, NOW(), NOW()),
  (gen_random_uuid(), p_hdd4tb,  22, 0, NOW(), NOW()),
  (gen_random_uuid(), p_ups,     10, 0, NOW(), NOW()),
  (gen_random_uuid(), p_hub,     35, 0, NOW(), NOW()),
  (gen_random_uuid(), p_qi15w,   45, 0, NOW(), NOW()),
  (gen_random_uuid(), p_switch,  15, 0, NOW(), NOW()),
  (gen_random_uuid(), p_ds5blco, 30, 0, NOW(), NOW()),
  (gen_random_uuid(), p_ds5edge,  8, 0, NOW(), NOW()),
  (gen_random_uuid(), p_xbox_pad,25, 0, NOW(), NOW()),
  (gen_random_uuid(), p_elden,   40, 0, NOW(), NOW()),
  (gen_random_uuid(), p_gow,     35, 0, NOW(), NOW()),
  (gen_random_uuid(), p_dxracer, 12, 0, NOW(), NOW()),
  (gen_random_uuid(), p_ergotron,18, 0, NOW(), NOW()),
  (gen_random_uuid(), p_razerbase,28,0, NOW(), NOW()),
  (gen_random_uuid(), p_mpad_xxl,60, 0, NOW(), NOW()),
  (gen_random_uuid(), p_brio,    22, 0, NOW(), NOW()),
  (gen_random_uuid(), p_yeti,    28, 0, NOW(), NOW()),
  (gen_random_uuid(), p_streamdk,20, 0, NOW(), NOW()),
  (gen_random_uuid(), p_elgato4k,12, 0, NOW(), NOW()),
  (gen_random_uuid(), p_ringlt,  35, 0, NOW(), NOW()),
  (gen_random_uuid(), p_canonm50, 7, 0, NOW(), NOW()),
  (gen_random_uuid(), p_asusrog,  9, 0, NOW(), NOW()),
  (gen_random_uuid(), p_z623,    18, 0, NOW(), NOW()),
  (gen_random_uuid(), p_hdmi21,  80, 0, NOW(), NOW()),
  (gen_random_uuid(), p_sony_wh, 20, 0, NOW(), NOW()),
  (gen_random_uuid(), p_gpro,    22, 0, NOW(), NOW()),
  (gen_random_uuid(), p_g502,    35, 0, NOW(), NOW()),
  (gen_random_uuid(), p_mxvert,  30, 0, NOW(), NOW()),
  (gen_random_uuid(), p_hyper,   40, 0, NOW(), NOW()),
  (gen_random_uuid(), p_k6,      25, 0, NOW(), NOW()),
  (gen_random_uuid(), p_ducky,   20, 0, NOW(), NOW()),
  (gen_random_uuid(), p_strider, 45, 0, NOW(), NOW()),
  (gen_random_uuid(), p_artisan, 15, 0, NOW(), NOW()),
  (gen_random_uuid(), p_arctis,  18, 0, NOW(), NOW()),
  (gen_random_uuid(), p_hs80,    16, 0, NOW(), NOW());

  -- ────────────────────────────────────────────────────────
  -- 9. OFERTAS ACTIVAS (descuentos sobre productos seleccionados)
  -- ────────────────────────────────────────────────────────
  INSERT INTO offers (id, product_id, name, discount_type, discount_value, discounted_price, start_date, end_date, status, created_at, updated_at) VALUES
  (gen_random_uuid(), p_rx7800,  'Promo AMD', 'PERCENTAGE', 10, ROUND(2420168 * 0.9), NOW() - INTERVAL '2 days', NOW() + INTERVAL '5 days',  'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), p_ddr4_16, 'Deal RAM DDR4', 'PERCENTAGE', 15, ROUND(258824 * 0.85), NOW() - INTERVAL '1 day', NOW() + INTERVAL '7 days', 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), p_argbled, 'Flash Sale RGB', 'PERCENTAGE', 20, ROUND(71849 * 0.80), NOW() - INTERVAL '3 days', NOW() + INTERVAL '2 days', 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), p_ds5blco, 'Promo PlayStation', 'PERCENTAGE', 8, ROUND(287395 * 0.92), NOW() - INTERVAL '1 day', NOW() + INTERVAL '10 days','ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), p_elden,   'Oferta Gamer', 'PERCENTAGE', 12, ROUND(258824 * 0.88), NOW() - INTERVAL '2 days', NOW() + INTERVAL '3 days', 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), p_gow,     'Weekend Deal', 'PERCENTAGE', 10, ROUND(196639 * 0.90), NOW() - INTERVAL '1 day', NOW() + INTERVAL '6 days',  'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), p_g502,    'Sale Logitech', 'PERCENTAGE', 15, ROUND(143697 * 0.85), NOW() - INTERVAL '4 days', NOW() + INTERVAL '3 days', 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), p_mpad_xxl,'Combo Pad',    'PERCENTAGE', 10, ROUND(75000  * 0.90), NOW() - INTERVAL '1 day', NOW() + INTERVAL '14 days', 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), p_hdmi21,  'Cable Deal',   'PERCENTAGE', 25, ROUND(39706  * 0.75), NOW() - INTERVAL '2 days', NOW() + INTERVAL '5 days',  'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), p_ringlt,  'Creator Sale', 'PERCENTAGE', 18, ROUND(203361 * 0.82), NOW() - INTERVAL '1 day', NOW() + INTERVAL '8 days',  'ACTIVE', NOW(), NOW());

  -- ────────────────────────────────────────────────────────
  -- 10. RESEÑAS (ratings 3-5 estrellas, distribuidas)
  -- ────────────────────────────────────────────────────────
  INSERT INTO reviews (id, product_id, user_id, order_id, rating, title, body, status, created_at, updated_at) VALUES
  -- RTX 4070 Super
  (gen_random_uuid(), p_rtx4070, buyer1, NULL, 5,
   'La GPU definitiva para 1440p', 'Increíble rendimiento en todos mis juegos a 1440p. Cyberpunk 2077 corre a más de 100fps con ray tracing activado. DLSS 3 es magia. La compré en TechStore Colombia y llegó bien empacada con garantía. 100% recomendada.',
   'APPROVED', NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days'),
  (gen_random_uuid(), p_rtx4070, buyer2, NULL, 4,
   'Excelente desempeño, precio justo', 'Actualicé de una RX 5700 XT y la diferencia es abismal. 1440p a 144Hz en todos los juegos modernos sin problema. La única pega es el consumo energético, necesitarás una buena fuente. Vendedor muy serio y envío rápido.',
   'APPROVED', NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days'),

  -- RX 7800 XT
  (gen_random_uuid(), p_rx7800,  buyer2, NULL, 5,
   '16GB de VRAM marcan la diferencia', 'Para quien juega en 1440p y quiere tener el mayor VRAM del mercado en este rango de precio, la RX 7800 XT es imbatible. Drivers AMD mejorados mucho en el último año. Llegó en perfectas condiciones.',
   'APPROVED', NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days'),

  -- CPU Intel i7-14700K
  (gen_random_uuid(), p_i7_14k,  buyer1, NULL, 5,
   'Bestia en multitarea y gaming', 'Con 20 núcleos este procesador no tiene rival en gaming y productividad simultánea. Renderizado en DaVinci Resolve y gaming a la vez sin problema. Lo combiné con la placa MSI B650 del mismo vendedor. Altamente recomendado.',
   'APPROVED', NOW() - INTERVAL '8 days', NOW() - INTERVAL '8 days'),

  -- Ryzen 7 7700X
  (gen_random_uuid(), p_r7_7700x, buyer2, NULL, 4,
   'Eficiente y potente en AM5', 'Migré a AM5 con este Ryzen y no me arrepiento. Arquitectura Zen 4 es un salto generacional. Temperaturas bajo control con un buen cooler. El soporte para DDR5 lo hace preparado para el futuro. Muy buen vendedor, proceso sin problemas.',
   'APPROVED', NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days'),

  -- Monitor LG 4K OLED
  (gen_random_uuid(), p_lg4k,    buyer1, NULL, 5,
   'El mejor monitor que he tenido en mi vida', 'El OLED cambia todo. Negros perfectos, colores increíbles, 144Hz. Ver cualquier contenido en este monitor es una experiencia distinta. Para gaming y trabajo creativo no hay nada mejor. Vale cada peso.',
   'APPROVED', NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days'),
  (gen_random_uuid(), p_lg4k,    buyer2, NULL, 5,
   'OLED gaming es el futuro', 'Venía de un IPS genérico y el salto a OLED es impresionante. Tiempos de respuesta instantáneos, sin ghosting, HDR real. El único cuidado es el burn-in pero con uso normal no hay problema.',
   'APPROVED', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),

  -- Monitor MSI 144Hz
  (gen_random_uuid(), p_mon144,  buyer1, NULL, 4,
   'Relación precio-calidad excelente', 'Para el precio que tiene, este monitor da un panel IPS de calidad con 165Hz y 1ms. Los colores son precisos y el diseño es bonito. Para gaming casual y trabajo diario es perfecto. Llegó bien empacado desde Medellín.',
   'APPROVED', NOW() - INTERVAL '14 days', NOW() - INTERVAL '14 days'),

  -- Nintendo Switch OLED
  (gen_random_uuid(), p_switch,  buyer2, NULL, 5,
   'La mejor forma de jugar en cualquier lugar', 'La pantalla OLED es espectacular para el modo portátil. Jugué Zelda TOTK en el metro y la experiencia es increíble. La base con LAN es un detalle genial. La tienda de Maria tiene los mejores precios de Colombia en Nintendo.',
   'APPROVED', NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days'),
  (gen_random_uuid(), p_switch,  buyer1, NULL, 4,
   'Ideal como regalo gamer', 'La compré como regalo de cumpleaños y fue un éxito total. Llegó sellada de fábrica, empaque original perfecto. La pantalla OLED se ve hermosa comparada con la Switch normal. Muy satisfecho.',
   'APPROVED', NOW() - INTERVAL '22 days', NOW() - INTERVAL '22 days'),

  -- DualSense PS5
  (gen_random_uuid(), p_ds5blco, buyer1, NULL, 5,
   'Los gatillos adaptativos son pura magia', 'Compré un repuesto para tener siempre uno cargando. Los juegos que aprovechan el DualSense (Returnal, Astro''s Playroom, Spider-Man 2) se sienten completamente diferentes. La háptica es de otro nivel. Vendedor rápido y confiable.',
   'APPROVED', NOW() - INTERVAL '16 days', NOW() - INTERVAL '16 days'),

  -- Elden Ring
  (gen_random_uuid(), p_elden,   buyer2, NULL, 5,
   'El mejor juego de la generación', 'Platinado en PS5. Más de 200 horas de contenido, mundo diseñado con maestría, dificultad justa. El DLC Shadow of the Erdtree añade otras 40 horas de contenido top tier. Bundle perfecto para empezar.',
   'APPROVED', NOW() - INTERVAL '9 days', NOW() - INTERVAL '9 days'),
  (gen_random_uuid(), p_elden,   buyer1, NULL, 4,
   'Imprescindible para cualquier gamer', 'Ya lo tenía en Xbox pero lo quería en colección física para PS5. La edición con DLC incluido es la compra perfecta. Envío muy rápido de Gamer Paradise.',
   'APPROVED', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),

  -- God of War
  (gen_random_uuid(), p_gow,     buyer1, NULL, 5,
   'Narrativa cinematográfica en videojuego', 'Kratos y Atreus en Ragnarök superan al anterior en todos los sentidos. Historia magistral, gameplay perfecto, gráficos impresionantes. El DLC Valhalla añade más horas de calidad. Imprescindible.',
   'APPROVED', NOW() - INTERVAL '28 days', NOW() - INTERVAL '28 days'),

  -- Silla DXRacer
  (gen_random_uuid(), p_dxracer, buyer2, NULL, 4,
   'Cómoda para sesiones largas', 'Llevo 6 meses usándola mínimo 8 horas al día y sigue en perfectas condiciones. El soporte lumbar ajustable hace una diferencia real. El montaje tardó unos 45 minutos pero vale la pena. Para el precio es de las mejores opciones.',
   'APPROVED', NOW() - INTERVAL '45 days', NOW() - INTERVAL '45 days'),

  -- Webcam Logitech Brio
  (gen_random_uuid(), p_brio,    buyer1, NULL, 5,
   'La mejor webcam del mercado para streaming', 'Calidad de imagen increíble en 1080p 60fps para streaming en Twitch. El HDR automático funciona perfectamente con iluminación variable. La compatibilidad con OBS es inmediata. NextGen Gaming tiene los mejores precios en equipos de streaming.',
   'APPROVED', NOW() - INTERVAL '17 days', NOW() - INTERVAL '17 days'),

  -- Blue Yeti
  (gen_random_uuid(), p_yeti,    buyer2, NULL, 5,
   'Audio profesional plug-and-play', 'Plug-and-play sin drivers, reconocido al instante en Windows. La diferencia con los headsets integrados es brutal. Patrón cárdioide perfecto para gaming y podcasts. Mi chat de Discord suena como una radio. Laura de NextGen Gaming atendió todas mis dudas antes de comprar.',
   'APPROVED', NOW() - INTERVAL '11 days', NOW() - INTERVAL '11 days'),
  (gen_random_uuid(), p_yeti,    buyer1, NULL, 4,
   'El estándar para streamers principiantes', 'Si estás empezando en streaming el Yeti es la compra más inteligente. Sin necesidad de interfaz de audio externa, calidad de sobra para Twitch o YouTube. El soporte anti-vibración incluido ayuda.',
   'APPROVED', NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days'),

  -- Stream Deck
  (gen_random_uuid(), p_streamdk,buyer1, NULL, 5,
   'Transforma completamente mi flujo de streaming', 'Cambios de escena, efectos de sonido, control de Spotify, respuestas de chat automatizadas... Todo con un solo botón. Una vez que lo usas no entiendes cómo hacías streams sin él. Software muy bien hecho.',
   'APPROVED', NOW() - INTERVAL '23 days', NOW() - INTERVAL '23 days'),

  -- Mouse G Pro X Superlight 2
  (gen_random_uuid(), p_gpro,    buyer2, NULL, 5,
   '60g de puro rendimiento competitivo', 'Venía de un Zowie EC2 y este es otro nivel. 60g se notan muchísimo en partidas largas de Valorant y CS2. El sensor HERO 2 es impecable a 1600 DPI con 400Hz de reporte. Juan de Perifericos Pro me lo entregó al día siguiente.',
   'APPROVED', NOW() - INTERVAL '13 days', NOW() - INTERVAL '13 days'),
  (gen_random_uuid(), p_gpro,    buyer1, NULL, 5,
   'El mouse que usan los pros no es casualidad', 'En competitivo de CS2 se nota la diferencia. Sin cables, sin lag perceptible, ligerísimo. El PTFE de los pies es premium de verdad. Precio alto pero justificado para quien se lo toma en serio.',
   'APPROVED', NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'),

  -- Keychron K6 Pro
  (gen_random_uuid(), p_k6,      buyer2, NULL, 5,
   'El mejor teclado 65% del mercado', 'Hotswap + Bluetooth + batería larga + RGB sur-facing = perfección. Cambié los switches a Boba U4 sin soldar en 20 minutos. El sonido con los foam dampeners es muy agradable. Compatible con Mac y Windows nativamente. Compra segura.',
   'APPROVED', NOW() - INTERVAL '19 days', NOW() - INTERVAL '19 days'),

  -- HyperX Alloy Origins
  (gen_random_uuid(), p_hyper,   buyer1, NULL, 4,
   'Sólido, confiable, rápido', 'Frame de aluminio que da una solidez impresionante. Los switches HyperX Red son muy suaves para gaming rápido. RGB bonito y configuración fácil con el software. Cable USB-C desmontable es un plus muy valorable en teclados gaming. Buen precio en Perifericos Pro.',
   'APPROVED', NOW() - INTERVAL '35 days', NOW() - INTERVAL '35 days'),

  -- Sony WH-1000XM5
  (gen_random_uuid(), p_sony_wh, buyer2, NULL, 5,
   'El ANC más efectivo que existe', 'Los uso todos los días en oficina abierta y en el metro. El ANC anula prácticamente todo el ruido ambiental. 30 horas de batería con ANC activo es brutal. LDAC para calidad de audio hi-fi con Android. Laura de NextGen Gaming los tiene mejor precio que Falabella.',
   'APPROVED', NOW() - INTERVAL '32 days', NOW() - INTERVAL '32 days'),
  (gen_random_uuid(), p_sony_wh, buyer1, NULL, 5,
   'Los mejores auriculares inalámbricos del planeta', 'Tenía los XM4 y el salto a los XM5 es notable. ANC mejorado, diseño más elegante (sin bisagras), sonido más natural. La única pega es que no se pueden plegar como los XM4 pero lo compensa todo lo demás.',
   'APPROVED', NOW() - INTERVAL '26 days', NOW() - INTERVAL '26 days'),

  -- Corsair HS80
  (gen_random_uuid(), p_hs80,    buyer1, NULL, 4,
   '65 horas de batería son reales', 'Comprobado: 65 horas de uso continuo sin RGB. El sonido es claro y los bajos están bien equilibrados. El micrófono omnidireccional desmontable es de muy buena calidad para Discord. El RGB CAPELLIX brilla mucho sin consumir batería significativa. Muy satisfecho.',
   'APPROVED', NOW() - INTERVAL '40 days', NOW() - INTERVAL '40 days'),

  -- Arctis Nova Pro
  (gen_random_uuid(), p_arctis,  buyer2, NULL, 5,
   'El headset gaming más premium que existe', 'El DAC externo con pantalla OLED es increíble. El ANC de alta fidelidad funciona tan bien como el Sony WH-1000XM5 en mi experiencia. Dos baterías intercambiables = nunca se queda sin carga. Para PC gaming no hay nada igual.',
   'APPROVED', NOW() - INTERVAL '50 days', NOW() - INTERVAL '50 days');

END $$;
