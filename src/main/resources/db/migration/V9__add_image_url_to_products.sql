-- V9__add_image_url_to_products.sql
-- Add image_url column to products table

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(2048);
