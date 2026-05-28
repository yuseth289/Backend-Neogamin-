-- ============================================================
-- V26: Elimina las marcas sembradas automáticamente en V25.
-- Las marcas se gestionan desde el panel de administración.
-- ============================================================

DELETE FROM brands WHERE slug IN ('razer','logitech-g','asus-rog','sony','samsung');
