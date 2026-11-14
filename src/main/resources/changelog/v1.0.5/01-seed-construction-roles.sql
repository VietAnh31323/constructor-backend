--liquibase formatted sql

--changeset constructor:v1.0.5-01-seed-construction-roles
-- Xóa check constraint cũ do Hibernate ddl-auto sinh trước đây (chỉ cho phép tập role cũ)
ALTER TABLE role DROP CONSTRAINT IF EXISTS role_name_check;

INSERT INTO role (name, description, created_at, created_by)
VALUES
    ('ADMIN', 'Quản trị viên', NOW(), 'SYSTEM'),
    ('DIRECTOR', 'Giám đốc', NOW(), 'SYSTEM'),
    ('PROJECT_MANAGER', 'Quản lý dự án (PM)', NOW(), 'SYSTEM'),
    ('SITE_ENGINEER', 'Kỹ sư giám sát / Chỉ huy trưởng', NOW(), 'SYSTEM'),
    ('ACCOUNTANT', 'Kế toán', NOW(), 'SYSTEM'),
    ('ESTIMATOR', 'Dự toán', NOW(), 'SYSTEM'),
    ('WAREHOUSE', 'Kho', NOW(), 'SYSTEM'),
    ('CUSTOMER_CARE', 'Chăm sóc khách hàng', NOW(), 'SYSTEM'),
    ('CUSTOMER', 'Khách hàng', NOW(), 'SYSTEM'),
    ('STAFF', 'Nhân viên', NOW(), 'SYSTEM')
ON CONFLICT (name) DO UPDATE
SET description = EXCLUDED.description,
    updated_at = NOW(),
    updated_by = 'SYSTEM';

-- Chuyển các gán quyền từ role cũ không còn trong enum ERole (ví dụ USER) sang STAFF
UPDATE account_role_map arm
SET role_id = (SELECT id FROM role WHERE name = 'STAFF' LIMIT 1)
WHERE arm.role_id IN (
    SELECT id FROM role
    WHERE name IS NULL OR name NOT IN (
        'ADMIN', 'DIRECTOR', 'PROJECT_MANAGER', 'SITE_ENGINEER', 'ACCOUNTANT',
        'ESTIMATOR', 'WAREHOUSE', 'CUSTOMER_CARE', 'CUSTOMER', 'STAFF'
    )
)
AND NOT EXISTS (
    SELECT 1 FROM account_role_map existing
    WHERE existing.account_id = arm.account_id
      AND existing.role_id = (SELECT id FROM role WHERE name = 'STAFF' LIMIT 1)
);

DELETE FROM account_role_map
WHERE role_id IN (
    SELECT id FROM role
    WHERE name IS NULL OR name NOT IN (
        'ADMIN', 'DIRECTOR', 'PROJECT_MANAGER', 'SITE_ENGINEER', 'ACCOUNTANT',
        'ESTIMATOR', 'WAREHOUSE', 'CUSTOMER_CARE', 'CUSTOMER', 'STAFF'
    )
);

DELETE FROM role
WHERE name IS NULL OR name NOT IN (
    'ADMIN', 'DIRECTOR', 'PROJECT_MANAGER', 'SITE_ENGINEER', 'ACCOUNTANT',
    'ESTIMATOR', 'WAREHOUSE', 'CUSTOMER_CARE', 'CUSTOMER', 'STAFF'
);
