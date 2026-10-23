-- V7__update_registration_otps_for_hashed_otp.sql
-- Widen otp column to hold BCrypt hashes (60 chars) and track resend timestamps.

-- Widen otp column from VARCHAR(10) to VARCHAR(60) for BCrypt hashes
ALTER TABLE registration_otps
    ALTER COLUMN otp TYPE VARCHAR(60);

-- Add last_requested_at to support resend rate-limiting; back-fill with created_at
ALTER TABLE registration_otps
    ADD COLUMN IF NOT EXISTS last_requested_at TIMESTAMPTZ;

UPDATE registration_otps
SET last_requested_at = created_at
WHERE last_requested_at IS NULL;

ALTER TABLE registration_otps
    ALTER COLUMN last_requested_at SET NOT NULL;
