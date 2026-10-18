--liquibase formatted sql

-- ============================================================================
-- Version: v1.0.4
-- Description: Chuẩn hóa kiểu dữ liệu tiền tệ sang NUMERIC(18,2)
-- Module: Database (Task ID 1.05)
-- ============================================================================

--changeset constructor:v1.0.4-01-alter-project-monetary-columns
ALTER TABLE project
    ALTER COLUMN contract_value TYPE NUMERIC(18, 2),
    ALTER COLUMN contract_advance TYPE NUMERIC(18, 2),
    ALTER COLUMN remaining_amount TYPE NUMERIC(18, 2);

--changeset constructor:v1.0.4-02-alter-project-line-monetary-columns
ALTER TABLE project_line
    ALTER COLUMN payment_amount TYPE NUMERIC(18, 2);
