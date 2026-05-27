-- V20: Fix product images — 50 unique primary URLs (all verified 200) + stable secondary images
-- Sources: 39 verified Unsplash IDs + 3 brand CDNs + 8 picsum/seed URLs (always-on)

DO $$
DECLARE pid UUID;
BEGIN

  -- 1. GPU NVIDIA GeForce RTX 4070 Super
  SELECT id INTO pid FROM products WHERE name ILIKE '%RTX 4070%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1509281373149-e957c6296406?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 2. GPU AMD Radeon RX 7800 XT
  SELECT id INTO pid FROM products WHERE name ILIKE '%RX 7800%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1573126617899-41f1dffb196c?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1587202372634-32705e3bf49c?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 3. CPU Intel Core i7-14700K
  SELECT id INTO pid FROM products WHERE name ILIKE '%i7-14700%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1545127398-14699f92334b?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 4. CPU AMD Ryzen 7 7700X
  SELECT id INTO pid FROM products WHERE name ILIKE '%Ryzen 7 7700%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1545127398-14699f92334b?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 5. Placa Base MSI MAG B650 Tomahawk
  SELECT id INTO pid FROM products WHERE name ILIKE '%B650 Tomahawk%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 6. RAM DDR5 32GB Corsair Vengeance
  SELECT id INTO pid FROM products WHERE name ILIKE '%DDR5%Corsair%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1432888498266-38ffec3eaf0a?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1448932223592-d1fc686e76ea?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 7. RAM DDR4 16GB Kingston Fury Beast
  SELECT id INTO pid FROM products WHERE name ILIKE '%DDR4%Kingston%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1555680202-c86f0e12f086?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1573126617899-41f1dffb196c?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 8. SSD NVMe M.2 Samsung 990 Pro
  SELECT id INTO pid FROM products WHERE name ILIKE '%Samsung 990 Pro%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1603481546238-487240415921?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 9. Gabinete NZXT H510 Flow
  SELECT id INTO pid FROM products WHERE name ILIKE '%NZXT H510%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1617854818583-09e7f077a156?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 10. Fuente 850W Seasonic Focus GX-850
  SELECT id INTO pid FROM products WHERE name ILIKE '%Seasonic%850%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1617854818583-09e7f077a156?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1603481546238-487240415921?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 11. Cooler CPU Torre Dark Rock Pro 4
  SELECT id INTO pid FROM products WHERE name ILIKE '%Dark Rock Pro 4%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 12. Cooler Líquido 360mm Corsair iCUE H150i
  SELECT id INTO pid FROM products WHERE name ILIKE '%Corsair iCUE H150i%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1587202372634-32705e3bf49c?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 13. Teclado Mecánico Gaming HyperX Alloy Origins
  SELECT id INTO pid FROM products WHERE name ILIKE '%HyperX Alloy Origins%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 14. Teclado Mecánico 65% Keychron K6 Pro
  SELECT id INTO pid FROM products WHERE name ILIKE '%Keychron K6%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1593642634315-48f5414c3ad9?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 15. Teclado Mecánico TKL Ducky One 3 Daybreak
  SELECT id INTO pid FROM products WHERE name ILIKE '%Ducky One 3%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1593642634315-48f5414c3ad9?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 16. Mouse Gaming Logitech G Pro X Superlight 2
  SELECT id INTO pid FROM products WHERE name ILIKE '%G Pro X Superlight%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 17. Mouse Gaming Logitech G502 X Plus
  SELECT id INTO pid FROM products WHERE name ILIKE '%G502 X Plus%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 18. Mouse Ergonómico Vertical Logitech MX Vertical
  SELECT id INTO pid FROM products WHERE name ILIKE '%MX Vertical%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1478737270239-2f02b77fc618?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 19. Mousepad XXL RGB SteelSeries QcK
  SELECT id INTO pid FROM products WHERE name ILIKE '%SteelSeries QcK%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 20. Mousepad Duro XL Razer Strider
  SELECT id INTO pid FROM products WHERE name ILIKE '%Razer Strider%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 21. Mousepad Control Speed Artisan Zero Soft XL
  SELECT id INTO pid FROM products WHERE name ILIKE '%Artisan Zero%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 22. Headset Gaming 7.1 SteelSeries Arctis Nova Pro
  SELECT id INTO pid FROM products WHERE name ILIKE '%Arctis Nova Pro%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1540829917886-91ab031b1764?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 23. Control PS5 DualSense Blanco
  SELECT id INTO pid FROM products WHERE name ILIKE '%DualSense Blanco%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://gmedia.playstation.com/is/image/SIEPDC/dualsense-white-image-block-01-en-29oct20?$1600px$' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 24. Control PS5 DualSense Edge
  SELECT id INTO pid FROM products WHERE name ILIKE '%DualSense Edge%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://gmedia.playstation.com/is/image/SIEPDC/dualsense-edge-image-block-01-en-7dec22?$1600px$' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 25. Control Xbox Series X Carbon Black
  SELECT id INTO pid FROM products WHERE name ILIKE '%Xbox Series X Carbon%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://img-prod-cms-rt-microsoft-com.akamaized.net/cms/api/am/imageFileData/RE5kx93?ver=d548' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 26. Nintendo Switch OLED Blanco
  SELECT id INTO pid FROM products WHERE name ILIKE '%Switch OLED%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1488590528505-98d2b5aba04b?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 27. Elden Ring PS5
  SELECT id INTO pid FROM products WHERE name ILIKE '%Elden Ring%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 28. God of War Ragnarök PS5
  SELECT id INTO pid FROM products WHERE name ILIKE '%God of War%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1611532736597-de2d4265fba3?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 29. Silla Gamer DXRacer Formula Series
  SELECT id INTO pid FROM products WHERE name ILIKE '%DXRacer%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1586880244406-556ebe35f282?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 30. Monitor 4K 27" LG OLED Gaming
  SELECT id INTO pid FROM products WHERE name ILIKE '%LG%OLED Gaming%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1593642634315-48f5414c3ad9?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 31. Monitor Gaming 27" MSI G274QPF
  SELECT id INTO pid FROM products WHERE name ILIKE '%MSI G274%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://picsum.photos/seed/msi-g274-monitor/800/800' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 32. Headset Inalámbrico Corsair HS80 Max RGB
  SELECT id INTO pid FROM products WHERE name ILIKE '%Corsair HS80%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1540829917886-91ab031b1764?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 33. Audífonos Bluetooth Sony WH-1000XM5
  SELECT id INTO pid FROM products WHERE name ILIKE '%Sony WH-1000XM5%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 34. Soporte Headset RGB Razer Base Station V2 Chroma
  SELECT id INTO pid FROM products WHERE name ILIKE '%Razer Base Station%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1608306448197-e83633f1261c?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 35. Micrófono USB Blue Yeti
  SELECT id INTO pid FROM products WHERE name ILIKE '%Blue Yeti%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1573164713988-8665fc963095?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1608306448197-e83633f1261c?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 36. Elgato Stream Deck MK.2
  SELECT id INTO pid FROM products WHERE name ILIKE '%Stream Deck%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1573164713988-8665fc963095?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 37. Webcam 4K Logitech Brio Ultra HD
  SELECT id INTO pid FROM products WHERE name ILIKE '%Logitech Brio%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1525385133512-2f3bdd039054?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 38. Cámara Canon EOS M50 Mark II
  SELECT id INTO pid FROM products WHERE name ILIKE '%Canon EOS M50%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1525385133512-2f3bdd039054?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 39. Ring Light LED 18" Neewer 55W
  SELECT id INTO pid FROM products WHERE name ILIKE '%Ring Light%Neewer%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1448932223592-d1fc686e76ea?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 40. Kit Iluminación ARGB Phanteks DRGB
  SELECT id INTO pid FROM products WHERE name ILIKE '%Phanteks DRGB%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1488590528505-98d2b5aba04b?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1448932223592-d1fc686e76ea?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 41. Parlantes 2.1 RGB Logitech Z623 THX
  SELECT id INTO pid FROM products WHERE name ILIKE '%Logitech Z623%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://picsum.photos/seed/logitech-z623-thx/800/800' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1540829917886-91ab031b1764?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 42. Router WiFi 6 ASUS ROG Rapture GT-AX6000
  SELECT id INTO pid FROM products WHERE name ILIKE '%ROG Rapture%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1544197150-b99a580bb7a8?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1573126617899-41f1dffb196c?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 43. SSD Externo 2TB Samsung T7 Shield
  SELECT id INTO pid FROM products WHERE name ILIKE '%Samsung T7 Shield%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1603481546238-487240415921?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 44. Disco Duro Externo 4TB Seagate Backup Plus
  SELECT id INTO pid FROM products WHERE name ILIKE '%Seagate Backup Plus%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://picsum.photos/seed/seagate-backup-4tb/800/800' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 45. Hub USB-C 10 en 1 Anker PowerExpand Elite
  SELECT id INTO pid FROM products WHERE name ILIKE '%Anker PowerExpand%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1587202372634-32705e3bf49c?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 46. Cable HDMI 2.1 8K
  SELECT id INTO pid FROM products WHERE name ILIKE '%Cable HDMI 2.1%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://picsum.photos/seed/hdmi21-cable-8k/800/800' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 47. Cargador Inalámbrico Qi 15W Belkin BoostCharge Pro
  SELECT id INTO pid FROM products WHERE name ILIKE '%Belkin BoostCharge%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://picsum.photos/seed/belkin-qi-charger/800/800' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1432888498266-38ffec3eaf0a?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 48. UPS 1500VA APC Back-UPS Pro 1500
  SELECT id INTO pid FROM products WHERE name ILIKE '%APC Back-UPS%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://picsum.photos/seed/apc-ups-1500va/800/800' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1544197150-b99a580bb7a8?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 49. Soporte Monitor Articulado VESA Ergotron LX
  SELECT id INTO pid FROM products WHERE name ILIKE '%Ergotron LX%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://picsum.photos/seed/ergotron-lx-monitor/800/800' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

  -- 50. Capturadora Elgato 4K60 Pro MK.2
  SELECT id INTO pid FROM products WHERE name ILIKE '%Elgato 4K60%' LIMIT 1;
  IF pid IS NOT NULL THEN
    UPDATE product_images SET url = 'https://picsum.photos/seed/elgato-4k60-cap/800/800' WHERE product_id = pid AND is_primary = TRUE;
    UPDATE product_images SET url = 'https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=800&h=800&fit=crop&q=80' WHERE product_id = pid AND is_primary = FALSE;
  END IF;

END $$;
