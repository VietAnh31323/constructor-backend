--liquibase formatted sql

-- ============================================================================
-- Version: v1.1.0
-- Description:
--   - Task 2.05: Phân loại khách hàng Cá nhân / Doanh nghiệp (MST, người đại diện, TK ngân hàng)
--   - Task 2.06: Liên kết khách hàng với dự án (project.customer_id FK + migrate dữ liệu project.owner)
--   - Task 2.07: Lịch sử chăm sóc khách hàng (bảng customer_care_log)
--   - Task 2.08: Pipeline trạng thái khách hàng (Tiềm năng -> Báo giá -> Ký HĐ -> Đang thi công -> Bảo hành)
-- Module: Khách hàng & Dự án
-- ============================================================================

--changeset constructor:v1.1.0-01-add-customer-classification-and-pipeline
ALTER TABLE customer DROP CONSTRAINT IF EXISTS customer_contact_status_check;
ALTER TABLE customer DROP CONSTRAINT IF EXISTS customer_customer_type_check;
ALTER TABLE customer DROP CONSTRAINT IF EXISTS customer_pipeline_stage_check;
ALTER TABLE customer DROP CONSTRAINT IF EXISTS fk_customer_assigned_staff;

ALTER TABLE customer
    ADD COLUMN IF NOT EXISTS customer_type           VARCHAR(30)  NOT NULL DEFAULT 'INDIVIDUAL',
    ADD COLUMN IF NOT EXISTS tax_code                VARCHAR(30),
    ADD COLUMN IF NOT EXISTS representative_name     VARCHAR(255),
    ADD COLUMN IF NOT EXISTS representative_position VARCHAR(100),
    ADD COLUMN IF NOT EXISTS id_card_number          VARCHAR(20),
    ADD COLUMN IF NOT EXISTS bank_account_number     VARCHAR(50),
    ADD COLUMN IF NOT EXISTS bank_name               VARCHAR(150),
    ADD COLUMN IF NOT EXISTS bank_branch             VARCHAR(150),
    ADD COLUMN IF NOT EXISTS bank_account_holder     VARCHAR(150),
    ADD COLUMN IF NOT EXISTS pipeline_stage          VARCHAR(40)  NOT NULL DEFAULT 'LEAD',
    ADD COLUMN IF NOT EXISTS assigned_staff_id       BIGINT;

ALTER TABLE customer
    ADD CONSTRAINT fk_customer_assigned_staff
        FOREIGN KEY (assigned_staff_id) REFERENCES staff (id) ON DELETE SET NULL;

UPDATE customer SET customer_type = 'INDIVIDUAL' WHERE customer_type IS NULL OR btrim(customer_type) = '';
UPDATE customer SET pipeline_stage = 'LEAD' WHERE pipeline_stage IS NULL OR btrim(pipeline_stage) = '';
UPDATE customer SET tax_code = NULL WHERE btrim(tax_code) = '';
UPDATE customer SET id_card_number = NULL WHERE btrim(id_card_number) = '';

CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_tax_code
    ON customer (tax_code)
    WHERE tax_code IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_customer_type ON customer (customer_type);
CREATE INDEX IF NOT EXISTS idx_customer_pipeline_stage ON customer (pipeline_stage);
CREATE INDEX IF NOT EXISTS idx_customer_assigned_staff_id ON customer (assigned_staff_id);

--changeset constructor:v1.1.0-02-link-project-customer-and-migrate-owner
ALTER TABLE project DROP CONSTRAINT IF EXISTS fk_project_customer;

ALTER TABLE project
    ADD COLUMN IF NOT EXISTS customer_id BIGINT;

ALTER TABLE project
    ADD CONSTRAINT fk_project_customer
        FOREIGN KEY (customer_id) REFERENCES customer (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_project_customer_id ON project (customer_id);

-- Bước 1: Ghép các dự án đã có project.owner trùng tên với khách hàng hiện có
UPDATE project p
SET customer_id = matched.customer_id
FROM (
    SELECT DISTINCT ON (lower(btrim(c.name)))
        lower(btrim(c.name)) AS norm_name,
        c.id                 AS customer_id
    FROM customer c
    WHERE c.deleted_at IS NULL
      AND c.name IS NOT NULL
      AND btrim(c.name) <> ''
    ORDER BY lower(btrim(c.name)), c.id ASC
) matched
WHERE p.customer_id IS NULL
  AND p.owner IS NOT NULL
  AND btrim(p.owner) <> ''
  AND lower(btrim(p.owner)) = matched.norm_name;

-- Bước 2: Tạo mới bản ghi khách hàng cho các giá trị project.owner chưa có trong bảng customer
INSERT INTO customer (
    name,
    customer_type,
    contact_status,
    pipeline_stage,
    is_potential,
    description,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT
    src.clean_owner,
    CASE
        WHEN src.clean_owner ~* '(công ty|cty|tập đoàn|doanh nghiệp|tnhh|cổ phần|ubnd|sở |ban )'
            THEN 'BUSINESS'
        ELSE 'INDIVIDUAL'
    END,
    'RESPONDED',
    'CONTRACT_SIGNED',
    TRUE,
    'Tự động đồng bộ từ thông tin Chủ đầu tư dự án cũ',
    NOW(),
    NOW(),
    'SYSTEM',
    'SYSTEM'
FROM (
    SELECT DISTINCT ON (lower(btrim(p.owner)))
        btrim(p.owner) AS clean_owner
    FROM project p
    WHERE p.customer_id IS NULL
      AND p.owner IS NOT NULL
      AND btrim(p.owner) <> ''
    ORDER BY lower(btrim(p.owner)), p.id ASC
) src
WHERE NOT EXISTS (
    SELECT 1
    FROM customer c
    WHERE c.deleted_at IS NULL
      AND c.name IS NOT NULL
      AND lower(btrim(c.name)) = lower(src.clean_owner)
);

-- Sinh mã KH cho các bản ghi vừa migrate chưa có code
UPDATE customer c
SET code = CASE
               WHEN EXISTS (
                   SELECT 1 FROM customer c2
                   WHERE c2.id <> c.id AND c2.code = ('KH' || c.id::text)
               ) THEN ('KH_M' || c.id::text)
               ELSE ('KH' || c.id::text)
           END
WHERE (c.code IS NULL OR btrim(c.code) = '');

-- Bước 3: Gán lại project.customer_id cho các dự án còn lại vừa tạo khách hàng
UPDATE project p
SET customer_id = matched.customer_id
FROM (
    SELECT DISTINCT ON (lower(btrim(c.name)))
        lower(btrim(c.name)) AS norm_name,
        c.id                 AS customer_id
    FROM customer c
    WHERE c.deleted_at IS NULL
      AND c.name IS NOT NULL
      AND btrim(c.name) <> ''
    ORDER BY lower(btrim(c.name)), c.id ASC
) matched
WHERE p.customer_id IS NULL
  AND p.owner IS NOT NULL
  AND btrim(p.owner) <> ''
  AND lower(btrim(p.owner)) = matched.norm_name;

-- Bước 4: Đồng bộ pipeline_stage khách hàng dựa trên trạng thái dự án liên kết
UPDATE customer c
SET pipeline_stage = proj_stage.target_stage
FROM (
    SELECT
        p.customer_id,
        CASE
            WHEN bool_or(p.state = 'IN_PROGRESS') THEN 'IN_CONSTRUCTION'
            WHEN bool_or(p.state = 'COMPLETED')   THEN 'WARRANTY'
            ELSE 'CONTRACT_SIGNED'
        END AS target_stage
    FROM project p
    WHERE p.deleted_at IS NULL
      AND p.customer_id IS NOT NULL
    GROUP BY p.customer_id
) proj_stage
WHERE c.id = proj_stage.customer_id
  AND (c.pipeline_stage IS NULL OR c.pipeline_stage = 'LEAD');

--changeset constructor:v1.1.0-03-create-table-customer-care-log
CREATE TABLE IF NOT EXISTS customer_care_log (
    id               BIGSERIAL PRIMARY KEY,
    customer_id      BIGINT      NOT NULL,
    staff_id         BIGINT,
    care_channel     VARCHAR(40) NOT NULL,
    care_time        TIMESTAMP   NOT NULL,
    summary          VARCHAR(255),
    content          TEXT        NOT NULL,
    outcome          VARCHAR(50),
    next_action_date TIMESTAMP,
    next_action_note VARCHAR(500),
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       VARCHAR,
    updated_by       VARCHAR,
    deleted_at       TIMESTAMP,
    deleted_by       VARCHAR,
    CONSTRAINT fk_customer_care_log_customer FOREIGN KEY (customer_id) REFERENCES customer (id) ON DELETE CASCADE,
    CONSTRAINT fk_customer_care_log_staff FOREIGN KEY (staff_id) REFERENCES staff (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_customer_care_log_customer_id ON customer_care_log (customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_care_log_staff_id ON customer_care_log (staff_id);
CREATE INDEX IF NOT EXISTS idx_customer_care_log_care_time ON customer_care_log (care_time DESC);
CREATE INDEX IF NOT EXISTS idx_customer_care_log_next_action ON customer_care_log (next_action_date);
CREATE INDEX IF NOT EXISTS idx_customer_care_log_deleted_at ON customer_care_log (deleted_at);
