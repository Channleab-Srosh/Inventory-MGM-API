-- V8__add_more_units_of_measure.sql
-- Update unit_of_measure check constraint to include additional units

ALTER TABLE products DROP CONSTRAINT IF EXISTS chk_products_unit_of_measure;

ALTER TABLE products
    ADD CONSTRAINT chk_products_unit_of_measure
    CHECK (unit_of_measure IN (
        'PCS', 'BOX', 'PACK', 'BOTTLE', 'CAN', 'KG', 'LITER',
        'DOZEN', 'CASE', 'CARTON', 'BUNDLE', 'BALE', 'SACK',
        'BAG', 'PALLET', 'ROLL', 'SET', 'PAIR', 'TRAY'
    ));
