-- V2__add_unit_of_measure_to_products.sql
-- Add unit of measurement (UOM) to products table

ALTER TABLE products
ADD COLUMN unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'PCS';

ALTER TABLE products
ADD CONSTRAINT chk_products_unit_of_measure
CHECK (unit_of_measure IN ('PCS', 'BOX', 'PACK', 'BOTTLE', 'CAN', 'KG', 'LITER'));

CREATE INDEX idx_products_unit_of_measure ON products (unit_of_measure);
