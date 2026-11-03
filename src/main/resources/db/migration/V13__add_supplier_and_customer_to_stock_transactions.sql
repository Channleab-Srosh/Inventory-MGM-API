-- V13__add_supplier_and_customer_to_stock_transactions.sql
-- Add supplier_id and customer_id columns to stock_transactions table

ALTER TABLE stock_transactions
    ADD COLUMN IF NOT EXISTS supplier_id BIGINT;

ALTER TABLE stock_transactions
    ADD COLUMN IF NOT EXISTS customer_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_transactions_supplier'
    ) THEN
        ALTER TABLE stock_transactions
            ADD CONSTRAINT fk_transactions_supplier
            FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_transactions_customer'
    ) THEN
        ALTER TABLE stock_transactions
            ADD CONSTRAINT fk_transactions_customer
            FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_transactions_supplier ON stock_transactions (supplier_id);
CREATE INDEX IF NOT EXISTS idx_transactions_customer ON stock_transactions (customer_id);

-- Backfill existing STOCK_IN records with product's supplier if null
UPDATE stock_transactions st
SET supplier_id = p.supplier_id
FROM products p
WHERE st.product_id = p.id
  AND st.transaction_type = 'STOCK_IN'
  AND st.supplier_id IS NULL
  AND p.supplier_id IS NOT NULL;

-- Backfill existing SALE STOCK_OUT records with first active customer if null
UPDATE stock_transactions st
SET customer_id = (SELECT id FROM customers WHERE active = true ORDER BY id ASC LIMIT 1)
WHERE st.transaction_type = 'STOCK_OUT'
  AND (st.reason = 'SALE' OR st.reason IS NULL)
  AND st.customer_id IS NULL
  AND EXISTS (SELECT 1 FROM customers WHERE active = true);
