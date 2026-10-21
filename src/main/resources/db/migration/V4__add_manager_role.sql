-- V4__add_manager_role.sql
-- Add MANAGER role to app_users role check constraint

ALTER TABLE app_users DROP CONSTRAINT IF EXISTS app_users_role_check;

ALTER TABLE app_users
    ADD CONSTRAINT app_users_role_check CHECK (role IN ('ADMIN', 'MANAGER', 'STAFF'));
