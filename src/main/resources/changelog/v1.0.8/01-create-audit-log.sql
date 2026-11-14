--liquibase formatted sql

-- ============================================================================
-- Version: v1.0.8
-- Description: Tạo bảng nhật ký hoạt động (audit_log) và phân quyền tra cứu
-- Module: Bảo mật (Task ID 1.14)
-- ============================================================================

--changeset constructor:v1.0.8-01-create-table-audit-log
CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(150),
    action      VARCHAR(50) NOT NULL,
    module      VARCHAR(100),
    entity_name VARCHAR(100),
    entity_id   VARCHAR(100),
    old_value   TEXT,
    new_value   TEXT,
    ip_address  VARCHAR(64),
    user_agent  TEXT,
    description TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    created_by  VARCHAR(150),
    updated_by  VARCHAR(150),
    deleted_at  TIMESTAMP,
    deleted_by  VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_username ON audit_log (username);
CREATE INDEX IF NOT EXISTS idx_audit_log_module ON audit_log (module);
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON audit_log (action);
CREATE INDEX IF NOT EXISTS idx_audit_log_entity ON audit_log (entity_name, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_deleted_at ON audit_log (deleted_at);

--changeset constructor:v1.0.8-02-seed-audit-log-permission
INSERT INTO permission (name, description, module, created_at, created_by)
VALUES ('AUDIT_LOG_VIEW', 'Xem nhật ký hoạt động hệ thống', 'SYSTEM', NOW(), 'SYSTEM')
ON CONFLICT (name) DO UPDATE
SET description = EXCLUDED.description,
    module = EXCLUDED.module,
    updated_at = NOW(),
    updated_by = 'SYSTEM';

INSERT INTO role_permission_map (role_id, permission_id, created_at, created_by)
SELECT r.id, p.id, NOW(), 'SYSTEM'
FROM role r, permission p
WHERE r.name IN ('ADMIN', 'DIRECTOR')
  AND p.name = 'AUDIT_LOG_VIEW'
ON CONFLICT (role_id, permission_id) DO NOTHING;
