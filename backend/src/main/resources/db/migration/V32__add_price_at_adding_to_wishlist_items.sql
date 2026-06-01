-- Agrega la columna price_at_adding a wishlist_items.
-- Registra el precio con IVA del producto en el momento de agregarlo,
-- permitiendo detectar bajadas de precio posteriores (campo onSale).
-- Nullable: items existentes quedan con NULL hasta su próxima actualización.

ALTER TABLE wishlist_items
    ADD COLUMN price_at_adding NUMERIC(14, 2);
