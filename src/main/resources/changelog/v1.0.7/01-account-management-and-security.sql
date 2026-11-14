--liquibase formatted sql

-- ============================================================================
-- Version: v1.0.7
-- Description: Bổ sung cột quản trị tài khoản, bắt đổi mật khẩu lần đầu,
--              giới hạn đăng nhập sai và thu hồi token (Task 1.12, 1.13)
-- ============================================================================

--changeset constructor:v1.0.7-01-add-account-security-columns
ALTER TABLE account
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP,
    ADD COLUMN IF NOT EXISTS tokens_valid_after TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_token_account_revoked ON token (account_id, revoked);
