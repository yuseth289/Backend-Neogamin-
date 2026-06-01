-- V33: Agregar campo icon_name a la tabla categories
-- Permite al administrador asociar un ícono de Lucide a cada categoría.
-- El valor almacenado es el nombre del ícono en camelCase (ej: lucideGamepad2).

ALTER TABLE categories
    ADD COLUMN icon_name VARCHAR(100);
