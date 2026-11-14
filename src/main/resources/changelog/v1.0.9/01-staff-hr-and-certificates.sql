--liquibase formatted sql

-- ============================================================================
-- Version: v1.0.9
-- Description: Bổ sung thông tin nhân sự (Task 2.01) và bảng chứng chỉ hành nghề (Task 2.02)
-- Module: Nhân viên
-- ============================================================================

--changeset constructor:v1.0.9-01-add-staff-hr-columns
ALTER TABLE staff DROP CONSTRAINT IF EXISTS staff_position_check;
ALTER TABLE staff DROP CONSTRAINT IF EXISTS staff_department_check;
ALTER TABLE staff DROP CONSTRAINT IF EXISTS staff_status_check;

ALTER TABLE staff
    ADD COLUMN IF NOT EXISTS position VARCHAR(50),
    ADD COLUMN IF NOT EXISTS department VARCHAR(100),
    ADD COLUMN IF NOT EXISTS join_date DATE,
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'WORKING',
    ADD COLUMN IF NOT EXISTS id_card_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS id_card_issue_date DATE,
    ADD COLUMN IF NOT EXISTS id_card_issue_place VARCHAR(255);

UPDATE staff SET status = 'WORKING' WHERE status IS NULL OR btrim(status) = '';
UPDATE staff SET id_card_number = NULL WHERE btrim(id_card_number) = '';

CREATE UNIQUE INDEX IF NOT EXISTS uk_staff_id_card_number
    ON staff (id_card_number)
    WHERE id_card_number IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_staff_position ON staff (position);
CREATE INDEX IF NOT EXISTS idx_staff_department ON staff (department);
CREATE INDEX IF NOT EXISTS idx_staff_status ON staff (status);

--changeset constructor:v1.0.9-02-create-table-staff-certificate
CREATE TABLE IF NOT EXISTS staff_certificate (
    id                 BIGSERIAL PRIMARY KEY,
    staff_id           BIGINT       NOT NULL,
    certificate_type   VARCHAR(100) NOT NULL,
    certificate_number VARCHAR(100) NOT NULL,
    certificate_rank   VARCHAR(50),
    issued_by          VARCHAR(255),
    issue_date         DATE         NOT NULL,
    expiry_date        DATE         NOT NULL,
    note               TEXT,
    created_at         TIMESTAMP,
    updated_at         TIMESTAMP,
    created_by         VARCHAR,
    updated_by         VARCHAR,
    deleted_at         TIMESTAMP,
    deleted_by         VARCHAR,
    CONSTRAINT fk_staff_certificate_staff FOREIGN KEY (staff_id) REFERENCES staff (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_staff_certificate_staff_id ON staff_certificate (staff_id);
CREATE INDEX IF NOT EXISTS idx_staff_certificate_expiry_date ON staff_certificate (expiry_date);
CREATE INDEX IF NOT EXISTS idx_staff_certificate_deleted_at ON staff_certificate (deleted_at);
CREATE UNIQUE INDEX IF NOT EXISTS uk_staff_certificate_number
    ON staff_certificate (certificate_number)
    WHERE deleted_at IS NULL;
