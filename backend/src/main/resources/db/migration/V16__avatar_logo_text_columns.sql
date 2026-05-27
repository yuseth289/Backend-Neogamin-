-- Ampliar columnas de imagen a TEXT para soportar URLs largas o base64
ALTER TABLE users   ALTER COLUMN avatar_url       TYPE TEXT;
ALTER TABLE sellers ALTER COLUMN store_logo_url   TYPE TEXT;
ALTER TABLE sellers ALTER COLUMN store_banner_url TYPE TEXT;
