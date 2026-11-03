-- V14__add_unit_cost_to_stock_transactions.sql
-- Add unit_cost column to stock_transactions table

ALTER TABLE stock_transactions
    ADD COLUMN IF NOT EXISTS unit_cost NUMERIC(12, 2);
