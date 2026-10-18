--liquibase formatted sql

--changeset constructor:v1.0.5-01-seed-construction-roles
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
