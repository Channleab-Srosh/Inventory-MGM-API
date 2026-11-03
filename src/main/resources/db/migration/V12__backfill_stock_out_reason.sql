-- V12__backfill_stock_out_reason.sql
-- Backfill reason = 'SALE' for all existing STOCK_OUT records that have no reason set.
-- These were created before the reason column was introduced (V11).

UPDATE stock_transactions
SET reason = 'SALE'
WHERE transaction_type = 'STOCK_OUT'
  AND reason IS NULL;
