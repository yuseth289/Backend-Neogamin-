-- Columna para reseñas editoriales creadas por admin
-- sin necesidad de compra verificada
ALTER TABLE reviews
  ADD COLUMN IF NOT EXISTS buyer_name_override VARCHAR(150);
