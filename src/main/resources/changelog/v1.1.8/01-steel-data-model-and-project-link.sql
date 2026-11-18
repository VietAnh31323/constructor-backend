--liquibase formatted sql

--changeset constructor:v1.1.8-01-drop-redundant-steel-tables
-- 3.01: Chuẩn hóa mô hình dữ liệu thép, bỏ các bảng summary/summary_table trùng lặp
DROP TABLE IF EXISTS summary_table_assembly_map CASCADE;
DROP TABLE IF EXISTS steel_project_table_map CASCADE;
DROP TABLE IF EXISTS summary_table CASCADE;
DROP TABLE IF EXISTS project_assembly_steel_map CASCADE;
DROP TABLE IF EXISTS summary CASCADE;

--changeset constructor:v1.1.8-02-steel-project-link-main-project
-- 3.02: Gắn dự án thép vào dự án chính
ALTER TABLE steel_project ADD COLUMN IF NOT EXISTS project_id BIGINT REFERENCES project(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_steel_project_project_id ON steel_project (project_id);

--changeset constructor:v1.1.8-03-normalize-steel-assembly-and-bars
-- 3.01 + 3.03: Thống nhất phân cấp Dự án thép -> Cấu kiện -> Thanh thép -> Đoạn uốn
ALTER TABLE steel_project_assembly_map ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0;
ALTER TABLE steel_project_assembly_map ADD COLUMN IF NOT EXISTS note TEXT;
ALTER TABLE steel_project_assembly_map ADD COLUMN IF NOT EXISTS total_length NUMERIC(18, 4) DEFAULT 0;
ALTER TABLE steel_project_assembly_map ADD COLUMN IF NOT EXISTS total_weight NUMERIC(18, 4) DEFAULT 0;

UPDATE steel_project_assembly_map
SET sort_order = id
WHERE sort_order IS NULL OR sort_order = 0;

CREATE INDEX IF NOT EXISTS idx_steel_project_assembly_map_project
    ON steel_project_assembly_map (steel_project_id, sort_order);

ALTER TABLE steel ADD COLUMN IF NOT EXISTS assembly_map_id BIGINT REFERENCES steel_project_assembly_map(id) ON DELETE CASCADE;
ALTER TABLE steel ADD COLUMN IF NOT EXISTS steel_category_id BIGINT REFERENCES steel_category(id) ON DELETE SET NULL;
ALTER TABLE steel ADD COLUMN IF NOT EXISTS bar_label VARCHAR(50);
ALTER TABLE steel ADD COLUMN IF NOT EXISTS bend_deduction NUMERIC(18, 2) DEFAULT 0;
ALTER TABLE steel ADD COLUMN IF NOT EXISTS cut_length NUMERIC(18, 4) DEFAULT 0;
ALTER TABLE steel ADD COLUMN IF NOT EXISTS unit_weight NUMERIC(18, 4) DEFAULT 0;
ALTER TABLE steel ADD COLUMN IF NOT EXISTS total_weight NUMERIC(18, 4) DEFAULT 0;
ALTER TABLE steel ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0;
ALTER TABLE steel ADD COLUMN IF NOT EXISTS note TEXT;

UPDATE steel
SET sort_order = id
WHERE sort_order IS NULL OR sort_order = 0;

UPDATE steel
SET bar_label = CAST(bar_code AS VARCHAR)
WHERE bar_label IS NULL AND bar_code IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_steel_assembly_map_id ON steel (assembly_map_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_steel_category_id ON steel (steel_category_id);
CREATE INDEX IF NOT EXISTS idx_steel_line_steel_id ON steel_line (steel_id);
