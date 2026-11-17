--liquibase formatted sql

--changeset constructor:v1.1.6-01-category-tree-columns
ALTER TABLE category ADD COLUMN IF NOT EXISTS parent_id BIGINT;
ALTER TABLE category ADD COLUMN IF NOT EXISTS unit VARCHAR(50);
ALTER TABLE category ADD COLUMN IF NOT EXISTS default_unit_price NUMERIC(18, 2) DEFAULT 0;
ALTER TABLE category ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_category_parent_id ON category (parent_id);

--changeset constructor:v1.1.6-02-project-category-quantity-price
ALTER TABLE project_category_map ADD COLUMN IF NOT EXISTS unit VARCHAR(50);
ALTER TABLE project_category_map ADD COLUMN IF NOT EXISTS quantity NUMERIC(18, 3) DEFAULT 1;
ALTER TABLE project_category_map ADD COLUMN IF NOT EXISTS unit_price NUMERIC(18, 2) DEFAULT 0;
ALTER TABLE project_category_map ADD COLUMN IF NOT EXISTS total_amount NUMERIC(18, 2) DEFAULT 0;
ALTER TABLE project_category_map ADD COLUMN IF NOT EXISTS note TEXT;

--changeset constructor:v1.1.6-03-create-process-templates
CREATE TABLE IF NOT EXISTS process_template (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(50)  NOT NULL,
    name          VARCHAR(255) NOT NULL,
    building_type VARCHAR(100) NOT NULL,
    description   TEXT,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    is_default    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(255),
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR(255),
    deleted_at    TIMESTAMP,
    deleted_by    VARCHAR(255),
    CONSTRAINT uq_process_template_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS process_template_stage (
    id            BIGSERIAL PRIMARY KEY,
    template_id   BIGINT       NOT NULL,
    name          VARCHAR(255) NOT NULL,
    sort_order    INTEGER      NOT NULL DEFAULT 1,
    duration_days INTEGER      NOT NULL DEFAULT 14,
    description   TEXT,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(255),
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR(255),
    deleted_at    TIMESTAMP,
    deleted_by    VARCHAR(255),
    CONSTRAINT fk_pts_template FOREIGN KEY (template_id) REFERENCES process_template (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS process_template_task (
    id            BIGSERIAL PRIMARY KEY,
    stage_id      BIGINT       NOT NULL,
    name          VARCHAR(255) NOT NULL,
    sort_order    INTEGER      NOT NULL DEFAULT 1,
    offset_days   INTEGER      NOT NULL DEFAULT 0,
    duration_days INTEGER      NOT NULL DEFAULT 7,
    priority      VARCHAR(30)  NOT NULL DEFAULT 'MEDIUM',
    description   TEXT,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(255),
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR(255),
    deleted_at    TIMESTAMP,
    deleted_by    VARCHAR(255),
    CONSTRAINT fk_ptt_stage FOREIGN KEY (stage_id) REFERENCES process_template_stage (id) ON DELETE CASCADE
);

--changeset constructor:v1.1.6-04-seed-default-process-templates
INSERT INTO process_template (code, name, building_type, description, is_active, is_default)
VALUES
    ('TPL-TOWNHOUSE', 'Quy trình thi công Nhà phố / Liền kề', 'TOWNHOUSE', 'Quy trình chuẩn cho nhà phố dân dụng từ ép cọc, móng, phần thân đến hoàn thiện bàn giao.', TRUE, TRUE),
    ('TPL-VILLA', 'Quy trình thi công Biệt thự / Nhà vườn', 'VILLA', 'Quy trình thi công biệt thự bao gồm phần ngầm, kết cấu mái, MEP thông minh và cảnh quan.', TRUE, TRUE),
    ('TPL-INDUSTRIAL', 'Quy trình thi công Nhà xưởng / Công nghiệp', 'INDUSTRIAL', 'Quy trình nhà xưởng kết cấu thép tiền chế: nền móng, lắp dựng kèo thép, bao che và PCCC.', TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

-- Seed stages & tasks for TOWNHOUSE
WITH tpl AS (SELECT id FROM process_template WHERE code = 'TPL-TOWNHOUSE'),
s1 AS (
    INSERT INTO process_template_stage (template_id, name, sort_order, duration_days)
    SELECT id, 'Giai đoạn 1: Chuẩn bị & Phần móng', 1, 20 FROM tpl
    RETURNING id
),
s2 AS (
    INSERT INTO process_template_stage (template_id, name, sort_order, duration_days)
    SELECT id, 'Giai đoạn 2: Kết cấu phần thân', 2, 45 FROM tpl
    RETURNING id
),
s3 AS (
    INSERT INTO process_template_stage (template_id, name, sort_order, duration_days)
    SELECT id, 'Giai đoạn 3: Xây tô, MEP & Hoàn thiện', 3, 35 FROM tpl
    RETURNING id
),
s4 AS (
    INSERT INTO process_template_stage (template_id, name, sort_order, duration_days)
    SELECT id, 'Giai đoạn 4: Nghiệm thu & Bàn giao', 4, 10 FROM tpl
    RETURNING id
)
INSERT INTO process_template_task (stage_id, name, sort_order, offset_days, duration_days, priority)
SELECT id, 'Giải phóng mặt bằng & Định vị tim cọc', 1, 0, 5, 'HIGH' FROM s1
UNION ALL
SELECT id, 'Ép cọc / Thi công móng & Đà kiềng', 2, 5, 10, 'URGENT' FROM s1
UNION ALL
SELECT id, 'Đổ bê tông móng & Hầm tự hoại', 3, 15, 5, 'HIGH' FROM s1
UNION ALL
SELECT id, 'Lắp dựng cốt thép, cốp pha cột & sàn các tầng', 1, 0, 30, 'HIGH' FROM s2
UNION ALL
SELECT id, 'Đổ bê tông sàn mái & Chống thấm sân thượng', 2, 30, 15, 'URGENT' FROM s2
UNION ALL
SELECT id, 'Xây tường bao, tường ngăn & Trát hoàn thiện', 1, 0, 15, 'MEDIUM' FROM s3
UNION ALL
SELECT id, 'Đi âm hệ thống Điện - Nước (MEP) & Sơn bả, lát gạch', 2, 15, 20, 'HIGH' FROM s3
UNION ALL
SELECT id, 'Tổng vệ sinh, nghiệm thu kỹ thuật & Bàn giao công trình', 1, 0, 10, 'HIGH' FROM s4;

-- Seed stages & tasks for VILLA
WITH tpl AS (SELECT id FROM process_template WHERE code = 'TPL-VILLA'),
s1 AS (
    INSERT INTO process_template_stage (template_id, name, sort_order, duration_days)
    SELECT id, 'Giai đoạn 1: Khảo sát, Pháp lý & Phần móng', 1, 25 FROM tpl
    RETURNING id
),
s2 AS (
    INSERT INTO process_template_stage (template_id, name, sort_order, duration_days)
    SELECT id, 'Giai đoạn 2: Kết cấu phần thô & Hệ mái', 2, 55 FROM tpl
    RETURNING id
),
s3 AS (
    INSERT INTO process_template_stage (template_id, name, sort_order, duration_days)
    SELECT id, 'Giai đoạn 3: Hoàn thiện cao cấp & Cảnh quan', 3, 45 FROM tpl
    RETURNING id
)
INSERT INTO process_template_task (stage_id, name, sort_order, offset_days, duration_days, priority)
SELECT id, 'Định vị ranh đất & Thi công móng băng / cọc khoan nhồi', 1, 0, 15, 'HIGH' FROM s1
UNION ALL
SELECT id, 'Thi công bể ngầm & Nền tầng trệt', 2, 15, 10, 'HIGH' FROM s1
UNION ALL
SELECT id, 'Thi công khung bê tông cốt thép các tầng', 1, 0, 40, 'URGENT' FROM s2
UNION ALL
SELECT id, 'Thi công hệ kèo mái ngói & Chống thấm', 2, 40, 15, 'HIGH' FROM s2
UNION ALL
SELECT id, 'Hoàn thiện nội thất, đá ốp lát & SmartHome', 1, 0, 30, 'HIGH' FROM s3
UNION ALL
SELECT id, 'Thi công sân vườn, cổng rào & Nghiệm thu bàn giao', 2, 30, 15, 'MEDIUM' FROM s3;

-- Seed stages & tasks for INDUSTRIAL
WITH tpl AS (SELECT id FROM process_template WHERE code = 'TPL-INDUSTRIAL'),
s1 AS (
    INSERT INTO process_template_stage (template_id, name, sort_order, duration_days)
    SELECT id, 'Giai đoạn 1: San lấp, Móng & Bulong neo', 1, 20 FROM tpl
    RETURNING id
),
s2 AS (
    INSERT INTO process_template_stage (template_id, name, sort_order, duration_days)
    SELECT id, 'Giai đoạn 2: Gia công & Lắp dựng kết cấu thép', 2, 30 FROM tpl
    RETURNING id
),
s3 AS (
    INSERT INTO process_template_stage (template_id, name, sort_order, duration_days)
    SELECT id, 'Giai đoạn 3: Bao che, Nền & PCCC', 3, 25 FROM tpl
    RETURNING id
)
INSERT INTO process_template_task (stage_id, name, sort_order, offset_days, duration_days, priority)
SELECT id, 'San lấp mặt bằng, lu lèn & Thi công móng đơn', 1, 0, 12, 'HIGH' FROM s1
UNION ALL
SELECT id, 'Lắp đặt hệ thống bulong neo & Đà kiềng', 2, 12, 8, 'URGENT' FROM s1
UNION ALL
SELECT id, 'Gia công cấu kiện thép tại xưởng & Sơn chống gỉ', 1, 0, 15, 'HIGH' FROM s2
UNION ALL
SELECT id, 'Cẩu lắp khung kèo thép, xà gồ & Giằng mái', 2, 15, 15, 'URGENT' FROM s2
UNION ALL
SELECT id, 'Lợp tôn mái, vách bao che & Đổ nền bê tông xoa mặt', 1, 0, 15, 'HIGH' FROM s3
UNION ALL
SELECT id, 'Lắp đặt hệ thống PCCC, điện công nghiệp & Nghiệm thu', 2, 15, 10, 'URGENT' FROM s3;
