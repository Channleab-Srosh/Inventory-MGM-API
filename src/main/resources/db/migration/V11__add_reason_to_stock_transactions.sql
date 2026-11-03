-- V11__add_reason_to_stock_transactions.sql
-- Add stock-out reason column to stock_transactions table

ALTER TABLE stock_transactions
    ADD COLUMN IF NOT EXISTS reason VARCHAR(20)
        CHECK (reason IN ('SALE', 'DAMAGE', 'OTHER'));
