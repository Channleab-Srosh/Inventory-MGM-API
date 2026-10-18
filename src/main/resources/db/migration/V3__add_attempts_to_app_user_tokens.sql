-- V3__add_attempts_to_app_user_tokens.sql
-- Add failed attempts tracking and composite index to app_user_tokens table

ALTER TABLE app_user_tokens
ADD COLUMN IF NOT EXISTS attempts INT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_user_tokens_user_revoked ON app_user_tokens (user_id, revoked);
