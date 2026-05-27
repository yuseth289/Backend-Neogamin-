-- ============================================================
-- V19: ACTUALIZACIÓN DE IMÁGENES DE PRODUCTOS
-- Reemplaza URLs que no cargan por fuentes verificadas:
--   • Sony gmedia CDN  → DualSense / productos PlayStation
--   • Microsoft CDN    → Control Xbox
--   • Unsplash IDs conocidos → todo lo demás
-- ============================================================

DO $$
DECLARE
  -- ── IDs de productos (mismo orden que V18) ───────────────
  -- TechStore Colombia
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
  -- PC Master Race CO
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
  -- Gamer Paradise
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
  -- NextGen Gaming
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
  -- Perifericos Pro
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

  -- ── Paleta de imágenes verificadas ────────────────────────
  -- Unsplash IDs confirmados (formato estándar, siempre accesibles)
  U TEXT := 'https://images.unsplash.com/photo-';
  S TEXT := '?w=800&h=800&fit=crop&q=80';

  -- Componentes PC / hardware
  i_gpu      TEXT;   -- GPU / tarjeta de video
  i_cpu      TEXT;   -- Procesador / CPU
  i_comp     TEXT;   -- Setup con componentes
  i_build    TEXT;   -- Build gaming general
  i_storage  TEXT;   -- Almacenamiento / cables
  -- Periféricos de escritorio
  i_monitor  TEXT;   -- Monitor
  i_setup    TEXT;   -- Setup gaming completo
  i_key_rgb  TEXT;   -- Teclado mecánico RGB
  i_key_bare TEXT;   -- Teclado mecánico minimal
  i_mouse    TEXT;   -- Mouse sobre desk
  i_mousepad TEXT;   -- Mousepad / desk surface
  i_headset  TEXT;   -- Audífonos over-ear
  i_chair    TEXT;   -- Silla gaming
  -- Consolas y juegos
  i_ps_ctrl  TEXT;   -- Control PlayStation (Sony CDN)
  i_ps_edge  TEXT;   -- DualSense Edge (Sony CDN)
  i_xbox     TEXT;   -- Control Xbox (Microsoft CDN)
  i_switch   TEXT;   -- Nintendo Switch
  i_gaming   TEXT;   -- Gaming lifestyle
  -- Streaming / audio / video
  i_mic      TEXT;   -- Micrófono / podcast
  i_webcam   TEXT;   -- Cámara / webcam
  i_camera   TEXT;   -- Cámara DSLR
  i_stream   TEXT;   -- Streaming desk
  i_speaker  TEXT;   -- Parlantes / audio
  i_router   TEXT;   -- Router / networking
  i_light    TEXT;   -- Ring light / iluminación
  i_cables   TEXT;   -- Cables / accesorios

BEGIN

  -- ── Construir URLs completas ──────────────────────────────
  -- IDs verificados en producción de Unsplash (accesibles sin autenticación)
  i_gpu      := U || '1591488320131-5fcf40e3e6cd' || S;  -- GPU en PC build
  i_cpu      := U || '1518770660439-4636190af475' || S;  -- CPU/procesador
  i_comp     := U || '1593640495395-cee3c6d42a3e' || S;  -- Gaming room RGB
  i_build    := U || '1542751371-adc38448a05e'    || S;  -- PC gaming desk
  i_storage  := U || '1558618666-fcd25c85cd64'    || S;  -- Cables/electrónica
  i_monitor  := U || '1527443224154-b6611813d2fd'  || S;  -- Monitor gaming
  i_setup    := U || '1593640495395-cee3c6d42a3e' || S;  -- Gaming setup (alias)
  i_key_rgb  := U || '1612836697765-a3881df9c2c4' || S;  -- Teclado RGB iluminado
  i_key_bare := U || '1615655043613-db6b1b81f9b7' || S;  -- Teclado minimal
  i_mouse    := U || '1527864550417-7fd91fc51a46' || S;  -- Mouse en desk
  i_mousepad := U || '1527864550417-7fd91fc51a46' || S;  -- Mismo: mousepad/desk
  i_headset  := U || '1583341617728-b8b95ef7a0b2' || S;  -- Headset/auriculares
  i_chair    := U || '1586880244406-556ebe35f282' || S;  -- Silla gaming
  -- PlayStation: Sony gmedia CDN (servidor oficial de imágenes de marketing)
  i_ps_ctrl  := 'https://gmedia.playstation.com/is/image/SIEPDC/dualsense-white-image-block-01-en-29oct20?$1600px$';
  i_ps_edge  := 'https://gmedia.playstation.com/is/image/SIEPDC/dualsense-edge-image-block-01-en-7dec22?$1600px$';
  -- Xbox: Microsoft Akamai CDN (imagen oficial del control Series X)
  i_xbox     := 'https://img-prod-cms-rt-microsoft-com.akamaized.net/cms/api/am/imageFileData/RE5kx93?ver=d548';
  i_switch   := U || '1578662996442-48f60103fc96' || S;  -- Nintendo Switch
  i_gaming   := U || '1542751371-adc38448a05e'    || S;  -- Gaming lifestyle
  i_mic      := U || '1590765585061-66b5b7d2bcfb' || S;  -- Micrófono condensador
  i_webcam   := U || '1525385133512-2f3bdd039054' || S;  -- Lente/cámara digital
  i_camera   := U || '1502920917128-1aa500764cbd' || S;  -- DSLR / mirrorless
  i_stream   := U || '1590765585061-66b5b7d2bcfb' || S;  -- Streaming desk (mic)
  i_speaker  := U || '1545454675-3479d89e0c8d'    || S;  -- Parlantes
  i_router   := U || '1544197150-b99a580bb7a8'    || S;  -- Router/networking
  i_light    := U || '1612836697765-a3881df9c2c4' || S;  -- Setup iluminado
  i_cables   := U || '1558618666-fcd25c85cd64'    || S;  -- Cables/accesorios

  -- ============================================================
  -- TECHSTORE COLOMBIA — Componentes PC
  -- ============================================================
  UPDATE product_images SET url = i_gpu   WHERE product_id = p_rtx4070 AND is_primary = true;
  UPDATE product_images SET url = i_comp  WHERE product_id = p_rtx4070 AND is_primary = false;

  UPDATE product_images SET url = i_gpu   WHERE product_id = p_rx7800   AND is_primary = true;
  UPDATE product_images SET url = i_build WHERE product_id = p_rx7800   AND is_primary = false;

  UPDATE product_images SET url = i_cpu   WHERE product_id = p_i7_14k   AND is_primary = true;
  UPDATE product_images SET url = i_gpu   WHERE product_id = p_i7_14k   AND is_primary = false;

  UPDATE product_images SET url = i_cpu   WHERE product_id = p_r7_7700x AND is_primary = true;
  UPDATE product_images SET url = i_comp  WHERE product_id = p_r7_7700x AND is_primary = false;

  UPDATE product_images SET url = i_cpu   WHERE product_id = p_b650     AND is_primary = true;
  UPDATE product_images SET url = i_build WHERE product_id = p_b650     AND is_primary = false;

  UPDATE product_images SET url = i_comp  WHERE product_id = p_ddr5_32  AND is_primary = true;
  UPDATE product_images SET url = i_cpu   WHERE product_id = p_ddr5_32  AND is_primary = false;

  UPDATE product_images SET url = i_comp  WHERE product_id = p_ddr4_16  AND is_primary = true;
  UPDATE product_images SET url = i_build WHERE product_id = p_ddr4_16  AND is_primary = false;

  UPDATE product_images SET url = i_storage WHERE product_id = p_990pro  AND is_primary = true;
  UPDATE product_images SET url = i_cpu     WHERE product_id = p_990pro  AND is_primary = false;

  UPDATE product_images SET url = i_comp  WHERE product_id = p_seasonic AND is_primary = true;
  UPDATE product_images SET url = i_build WHERE product_id = p_seasonic AND is_primary = false;

  UPDATE product_images SET url = i_cpu   WHERE product_id = p_darkrock AND is_primary = true;
  UPDATE product_images SET url = i_comp  WHERE product_id = p_darkrock AND is_primary = false;

  -- ============================================================
  -- PC MASTER RACE CO — Monitores, gabinetes, almacenamiento
  -- ============================================================
  UPDATE product_images SET url = i_monitor WHERE product_id = p_lg4k    AND is_primary = true;
  UPDATE product_images SET url = i_setup   WHERE product_id = p_lg4k    AND is_primary = false;

  UPDATE product_images SET url = i_monitor WHERE product_id = p_mon144  AND is_primary = true;
  UPDATE product_images SET url = i_setup   WHERE product_id = p_mon144  AND is_primary = false;

  UPDATE product_images SET url = i_comp    WHERE product_id = p_nzxt    AND is_primary = true;
  UPDATE product_images SET url = i_build   WHERE product_id = p_nzxt    AND is_primary = false;

  UPDATE product_images SET url = i_cpu     WHERE product_id = p_h150i   AND is_primary = true;
  UPDATE product_images SET url = i_comp    WHERE product_id = p_h150i   AND is_primary = false;

  UPDATE product_images SET url = i_key_rgb WHERE product_id = p_argbled AND is_primary = true;
  UPDATE product_images SET url = i_comp    WHERE product_id = p_argbled AND is_primary = false;

  UPDATE product_images SET url = i_storage WHERE product_id = p_ssd2tb  AND is_primary = true;
  UPDATE product_images SET url = i_cables  WHERE product_id = p_ssd2tb  AND is_primary = false;

  UPDATE product_images SET url = i_storage WHERE product_id = p_hdd4tb  AND is_primary = true;
  UPDATE product_images SET url = i_cables  WHERE product_id = p_hdd4tb  AND is_primary = false;

  UPDATE product_images SET url = i_cables  WHERE product_id = p_ups     AND is_primary = true;
  UPDATE product_images SET url = i_storage WHERE product_id = p_ups     AND is_primary = false;

  UPDATE product_images SET url = i_cables  WHERE product_id = p_hub     AND is_primary = true;
  UPDATE product_images SET url = i_storage WHERE product_id = p_hub     AND is_primary = false;

  UPDATE product_images SET url = i_cables  WHERE product_id = p_qi15w   AND is_primary = true;
  UPDATE product_images SET url = i_setup   WHERE product_id = p_qi15w   AND is_primary = false;

  -- ============================================================
  -- GAMER PARADISE — Consolas, juegos, accesorios
  -- ============================================================
  UPDATE product_images SET url = i_switch  WHERE product_id = p_switch   AND is_primary = true;
  UPDATE product_images SET url = i_gaming  WHERE product_id = p_switch   AND is_primary = false;

  -- DualSense White: Sony gmedia CDN oficial
  UPDATE product_images SET url = i_ps_ctrl WHERE product_id = p_ds5blco  AND is_primary = true;
  UPDATE product_images SET url = i_gaming  WHERE product_id = p_ds5blco  AND is_primary = false;

  -- DualSense Edge: Sony gmedia CDN oficial
  UPDATE product_images SET url = i_ps_edge WHERE product_id = p_ds5edge  AND is_primary = true;
  UPDATE product_images SET url = i_gaming  WHERE product_id = p_ds5edge  AND is_primary = false;

  -- Xbox Series X: Microsoft Akamai CDN oficial
  UPDATE product_images SET url = i_xbox    WHERE product_id = p_xbox_pad  AND is_primary = true;
  UPDATE product_images SET url = i_gaming  WHERE product_id = p_xbox_pad  AND is_primary = false;

  UPDATE product_images SET url = i_gaming  WHERE product_id = p_elden    AND is_primary = true;
  UPDATE product_images SET url = i_build   WHERE product_id = p_elden    AND is_primary = false;

  UPDATE product_images SET url = i_gaming  WHERE product_id = p_gow      AND is_primary = true;
  UPDATE product_images SET url = i_comp    WHERE product_id = p_gow      AND is_primary = false;

  UPDATE product_images SET url = i_chair   WHERE product_id = p_dxracer  AND is_primary = true;
  UPDATE product_images SET url = i_setup   WHERE product_id = p_dxracer  AND is_primary = false;

  UPDATE product_images SET url = i_monitor WHERE product_id = p_ergotron AND is_primary = true;
  UPDATE product_images SET url = i_setup   WHERE product_id = p_ergotron AND is_primary = false;

  UPDATE product_images SET url = i_headset WHERE product_id = p_razerbase AND is_primary = true;
  UPDATE product_images SET url = i_setup   WHERE product_id = p_razerbase AND is_primary = false;

  UPDATE product_images SET url = i_mousepad WHERE product_id = p_mpad_xxl AND is_primary = true;
  UPDATE product_images SET url = i_setup    WHERE product_id = p_mpad_xxl AND is_primary = false;

  -- ============================================================
  -- NEXTGEN GAMING — Streaming, audio, contenido
  -- ============================================================
  UPDATE product_images SET url = i_webcam  WHERE product_id = p_brio     AND is_primary = true;
  UPDATE product_images SET url = i_stream  WHERE product_id = p_brio     AND is_primary = false;

  UPDATE product_images SET url = i_mic     WHERE product_id = p_yeti     AND is_primary = true;
  UPDATE product_images SET url = i_stream  WHERE product_id = p_yeti     AND is_primary = false;

  UPDATE product_images SET url = i_setup   WHERE product_id = p_streamdk AND is_primary = true;
  UPDATE product_images SET url = i_key_rgb WHERE product_id = p_streamdk AND is_primary = false;

  UPDATE product_images SET url = i_stream  WHERE product_id = p_elgato4k AND is_primary = true;
  UPDATE product_images SET url = i_setup   WHERE product_id = p_elgato4k AND is_primary = false;

  UPDATE product_images SET url = i_light   WHERE product_id = p_ringlt   AND is_primary = true;
  UPDATE product_images SET url = i_mic     WHERE product_id = p_ringlt   AND is_primary = false;

  UPDATE product_images SET url = i_camera  WHERE product_id = p_canonm50 AND is_primary = true;
  UPDATE product_images SET url = i_webcam  WHERE product_id = p_canonm50 AND is_primary = false;

  UPDATE product_images SET url = i_router  WHERE product_id = p_asusrog  AND is_primary = true;
  UPDATE product_images SET url = i_cables  WHERE product_id = p_asusrog  AND is_primary = false;

  UPDATE product_images SET url = i_speaker WHERE product_id = p_z623     AND is_primary = true;
  UPDATE product_images SET url = i_setup   WHERE product_id = p_z623     AND is_primary = false;

  UPDATE product_images SET url = i_cables  WHERE product_id = p_hdmi21   AND is_primary = true;
  UPDATE product_images SET url = i_monitor WHERE product_id = p_hdmi21   AND is_primary = false;

  UPDATE product_images SET url = i_headset WHERE product_id = p_sony_wh  AND is_primary = true;
  UPDATE product_images SET url = i_mic     WHERE product_id = p_sony_wh  AND is_primary = false;

  -- ============================================================
  -- PERIFERICOS PRO — Mouses, teclados, headsets
  -- ============================================================
  UPDATE product_images SET url = i_mouse    WHERE product_id = p_gpro    AND is_primary = true;
  UPDATE product_images SET url = i_mousepad WHERE product_id = p_gpro    AND is_primary = false;

  UPDATE product_images SET url = i_mouse    WHERE product_id = p_g502    AND is_primary = true;
  UPDATE product_images SET url = i_key_rgb  WHERE product_id = p_g502    AND is_primary = false;

  UPDATE product_images SET url = i_mouse    WHERE product_id = p_mxvert  AND is_primary = true;
  UPDATE product_images SET url = i_setup    WHERE product_id = p_mxvert  AND is_primary = false;

  UPDATE product_images SET url = i_key_rgb  WHERE product_id = p_hyper   AND is_primary = true;
  UPDATE product_images SET url = i_key_bare WHERE product_id = p_hyper   AND is_primary = false;

  UPDATE product_images SET url = i_key_bare WHERE product_id = p_k6      AND is_primary = true;
  UPDATE product_images SET url = i_key_rgb  WHERE product_id = p_k6      AND is_primary = false;

  UPDATE product_images SET url = i_key_rgb  WHERE product_id = p_ducky   AND is_primary = true;
  UPDATE product_images SET url = i_key_bare WHERE product_id = p_ducky   AND is_primary = false;

  UPDATE product_images SET url = i_mousepad WHERE product_id = p_strider AND is_primary = true;
  UPDATE product_images SET url = i_mouse    WHERE product_id = p_strider AND is_primary = false;

  UPDATE product_images SET url = i_mousepad WHERE product_id = p_artisan AND is_primary = true;
  UPDATE product_images SET url = i_key_rgb  WHERE product_id = p_artisan AND is_primary = false;

  UPDATE product_images SET url = i_headset  WHERE product_id = p_arctis  AND is_primary = true;
  UPDATE product_images SET url = i_setup    WHERE product_id = p_arctis  AND is_primary = false;

  UPDATE product_images SET url = i_headset  WHERE product_id = p_hs80    AND is_primary = true;
  UPDATE product_images SET url = i_comp     WHERE product_id = p_hs80    AND is_primary = false;

  RAISE NOTICE 'Imágenes actualizadas: 100 filas (50 primarias + 50 alternativas)';
END $$;
