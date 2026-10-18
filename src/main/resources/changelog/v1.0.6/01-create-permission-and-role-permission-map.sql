--liquibase formatted sql

--changeset constructor:v1.0.6-01-create-table-permission
CREATE TABLE IF NOT EXISTS permission (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    module      VARCHAR(50),
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR,
    updated_by  VARCHAR,
    deleted_at  TIMESTAMP,
    deleted_by  VARCHAR
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_permission_name ON permission(name);

--changeset constructor:v1.0.6-02-create-table-role-permission-map
CREATE TABLE IF NOT EXISTS role_permission_map (
    id            BIGSERIAL PRIMARY KEY,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    created_by    VARCHAR,
    updated_by    VARCHAR,
    deleted_at    TIMESTAMP,
    deleted_by    VARCHAR,
    CONSTRAINT fk_role_permission_map_role FOREIGN KEY (role_id) REFERENCES role(id),
    CONSTRAINT fk_role_permission_map_permission FOREIGN KEY (permission_id) REFERENCES permission(id),
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
);

CREATE INDEX IF NOT EXISTS idx_role_permission_role_id ON role_permission_map(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permission_permission_id ON role_permission_map(permission_id);

--changeset constructor:v1.0.6-03-seed-permissions
INSERT INTO permission (name, description, module, created_at, created_by)
VALUES
    ('PROJECT_VIEW', 'Xem danh sách và chi tiết dự án', 'PROJECT', NOW(), 'SYSTEM'),
    ('PROJECT_CREATE', 'Tạo mới dự án', 'PROJECT', NOW(), 'SYSTEM'),
    ('PROJECT_UPDATE', 'Cập nhật thông tin dự án', 'PROJECT', NOW(), 'SYSTEM'),
    ('PROJECT_DELETE', 'Xóa dự án', 'PROJECT', NOW(), 'SYSTEM'),

    ('PROGRESS_VIEW', 'Xem tiến độ công trình', 'PROGRESS', NOW(), 'SYSTEM'),
    ('PROGRESS_UPDATE', 'Cập nhật tiến độ thi công', 'PROGRESS', NOW(), 'SYSTEM'),

    ('TASK_VIEW', 'Xem công việc', 'TASK', NOW(), 'SYSTEM'),
    ('TASK_CREATE', 'Giao và tạo công việc', 'TASK', NOW(), 'SYSTEM'),
    ('TASK_UPDATE', 'Cập nhật và báo cáo tiến độ công việc', 'TASK', NOW(), 'SYSTEM'),
    ('TASK_DELETE', 'Xóa công việc', 'TASK', NOW(), 'SYSTEM'),

    ('PAYMENT_VIEW', 'Xem hồ sơ thanh toán', 'FINANCE', NOW(), 'SYSTEM'),
    ('PAYMENT_CREATE', 'Tạo đề nghị thanh toán', 'FINANCE', NOW(), 'SYSTEM'),
    ('PAYMENT_APPROVE', 'Phê duyệt thanh toán và chi phí', 'FINANCE', NOW(), 'SYSTEM'),
    ('FINANCE_MANAGE', 'Quản lý tài chính dòng tiền dự án', 'FINANCE', NOW(), 'SYSTEM'),

    ('ESTIMATE_VIEW', 'Xem hồ sơ dự toán chi phí', 'ESTIMATE', NOW(), 'SYSTEM'),
    ('ESTIMATE_CREATE', 'Lập dự toán công trình', 'ESTIMATE', NOW(), 'SYSTEM'),
    ('ESTIMATE_UPDATE', 'Chỉnh sửa định mức và dự toán', 'ESTIMATE', NOW(), 'SYSTEM'),
    ('ESTIMATE_APPROVE', 'Phê duyệt bảng dự toán', 'ESTIMATE', NOW(), 'SYSTEM'),

    ('MATERIAL_VIEW', 'Xem vật tư và kho kết cấu thép', 'MATERIAL', NOW(), 'SYSTEM'),
    ('MATERIAL_MANAGE', 'Quản lý định mức và xuất nhập vật tư', 'MATERIAL', NOW(), 'SYSTEM'),

    ('CUSTOMER_VIEW', 'Xem thông tin khách hàng', 'CUSTOMER', NOW(), 'SYSTEM'),
    ('CUSTOMER_MANAGE', 'Thêm và quản lý khách hàng', 'CUSTOMER', NOW(), 'SYSTEM'),

    ('USER_VIEW', 'Xem danh sách tài khoản và nhân viên', 'USER', NOW(), 'SYSTEM'),
    ('USER_MANAGE', 'Quản lý tài khoản, nhân viên và phân quyền', 'USER', NOW(), 'SYSTEM')
ON CONFLICT (name) DO UPDATE
SET description = EXCLUDED.description,
    module = EXCLUDED.module,
    updated_at = NOW(),
    updated_by = 'SYSTEM';

--changeset constructor:v1.0.6-04-seed-role-permissions
-- Gán toàn bộ quyền cho ADMIN
INSERT INTO role_permission_map (role_id, permission_id, created_at, created_by)
SELECT r.id, p.id, NOW(), 'SYSTEM'
FROM role r, permission p
WHERE r.name = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- DIRECTOR (Giám đốc): Toàn quyền dự án, tiến độ, tài chính, phê duyệt
INSERT INTO role_permission_map (role_id, permission_id, created_at, created_by)
SELECT r.id, p.id, NOW(), 'SYSTEM'
FROM role r, permission p
WHERE r.name = 'DIRECTOR'
  AND p.name IN (
    'PROJECT_VIEW', 'PROJECT_CREATE', 'PROJECT_UPDATE', 'PROJECT_DELETE',
    'PROGRESS_VIEW', 'PROGRESS_UPDATE',
    'TASK_VIEW', 'TASK_CREATE', 'TASK_UPDATE', 'TASK_DELETE',
    'PAYMENT_VIEW', 'PAYMENT_APPROVE', 'FINANCE_MANAGE',
    'ESTIMATE_VIEW', 'ESTIMATE_APPROVE',
    'MATERIAL_VIEW', 'MATERIAL_MANAGE',
    'CUSTOMER_VIEW', 'CUSTOMER_MANAGE',
    'USER_VIEW'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- PROJECT_MANAGER (Quản lý dự án): Dự án, tiến độ, công việc, đề xuất thanh toán, xem dự toán, vật tư
INSERT INTO role_permission_map (role_id, permission_id, created_at, created_by)
SELECT r.id, p.id, NOW(), 'SYSTEM'
FROM role r, permission p
WHERE r.name = 'PROJECT_MANAGER'
  AND p.name IN (
    'PROJECT_VIEW', 'PROJECT_CREATE', 'PROJECT_UPDATE',
    'PROGRESS_VIEW', 'PROGRESS_UPDATE',
    'TASK_VIEW', 'TASK_CREATE', 'TASK_UPDATE', 'TASK_DELETE',
    'PAYMENT_VIEW', 'PAYMENT_CREATE',
    'ESTIMATE_VIEW',
    'MATERIAL_VIEW',
    'CUSTOMER_VIEW'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- SITE_ENGINEER (Kỹ sư giám sát / Chỉ huy trưởng): Tiến độ, công việc tại hiện trường, vật tư
INSERT INTO role_permission_map (role_id, permission_id, created_at, created_by)
SELECT r.id, p.id, NOW(), 'SYSTEM'
FROM role r, permission p
WHERE r.name = 'SITE_ENGINEER'
  AND p.name IN (
    'PROJECT_VIEW',
    'PROGRESS_VIEW', 'PROGRESS_UPDATE',
    'TASK_VIEW', 'TASK_CREATE', 'TASK_UPDATE',
    'MATERIAL_VIEW'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ACCOUNTANT (Kế toán): Thanh toán, tài chính, dự toán chi phí
INSERT INTO role_permission_map (role_id, permission_id, created_at, created_by)
SELECT r.id, p.id, NOW(), 'SYSTEM'
FROM role r, permission p
WHERE r.name = 'ACCOUNTANT'
  AND p.name IN (
    'PROJECT_VIEW',
    'PAYMENT_VIEW', 'PAYMENT_CREATE', 'PAYMENT_APPROVE', 'FINANCE_MANAGE',
    'ESTIMATE_VIEW'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ESTIMATOR (Dự toán): Dự toán công trình, vật tư định mức
INSERT INTO role_permission_map (role_id, permission_id, created_at, created_by)
SELECT r.id, p.id, NOW(), 'SYSTEM'
FROM role r, permission p
WHERE r.name = 'ESTIMATOR'
  AND p.name IN (
    'PROJECT_VIEW',
    'ESTIMATE_VIEW', 'ESTIMATE_CREATE', 'ESTIMATE_UPDATE',
    'MATERIAL_VIEW'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- WAREHOUSE (Kho): Vật tư, kết cấu thép, xuất nhập tồn
INSERT INTO role_permission_map (role_id, permission_id, created_at, created_by)
SELECT r.id, p.id, NOW(), 'SYSTEM'
FROM role r, permission p
WHERE r.name = 'WAREHOUSE'
  AND p.name IN (
    'PROJECT_VIEW',
    'MATERIAL_VIEW', 'MATERIAL_MANAGE'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- CUSTOMER_CARE (CSKH): Khách hàng, tiến độ công khai
INSERT INTO role_permission_map (role_id, permission_id, created_at, created_by)
SELECT r.id, p.id, NOW(), 'SYSTEM'
FROM role r, permission p
WHERE r.name = 'CUSTOMER_CARE'
  AND p.name IN (
    'PROJECT_VIEW',
    'CUSTOMER_VIEW', 'CUSTOMER_MANAGE',
    'PROGRESS_VIEW'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- CUSTOMER (Khách hàng): Xem dự án và tiến độ công trình của mình
INSERT INTO role_permission_map (role_id, permission_id, created_at, created_by)
SELECT r.id, p.id, NOW(), 'SYSTEM'
FROM role r, permission p
WHERE r.name = 'CUSTOMER'
  AND p.name IN (
    'PROJECT_VIEW',
    'PROGRESS_VIEW'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- STAFF (Nhân viên): Xem dự án, xem tiến độ, thực hiện công việc được giao
INSERT INTO role_permission_map (role_id, permission_id, created_at, created_by)
SELECT r.id, p.id, NOW(), 'SYSTEM'
FROM role r, permission p
WHERE r.name = 'STAFF'
  AND p.name IN (
    'PROJECT_VIEW',
    'PROGRESS_VIEW',
    'TASK_VIEW', 'TASK_UPDATE'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;
