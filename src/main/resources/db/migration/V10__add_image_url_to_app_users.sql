-- V10__add_image_url_to_app_users.sql
-- Add image_url column to app_users table for user profile pictures / avatars

ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(2048);
