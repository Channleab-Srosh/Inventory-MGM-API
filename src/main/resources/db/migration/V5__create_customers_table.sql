-- V5__create_customers_table.sql
-- Create customers table for Customer Management

CREATE TABLE IF NOT EXISTS customers (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    phone       VARCHAR(20)   NOT NULL,
    email       VARCHAR(100),
    address     VARCHAR(255),
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_customers_phone   ON customers (phone);
CREATE UNIQUE INDEX idx_customers_email   ON customers (LOWER(email)) WHERE email IS NOT NULL;
CREATE        INDEX idx_customers_name    ON customers (name);
