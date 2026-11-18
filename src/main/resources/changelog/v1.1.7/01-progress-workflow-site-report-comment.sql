--liquibase formatted sql

--changeset constructor:v1.1.7-01-project-progress-schedule
-- 2.21: Giai đoạn có thứ tự, ngày kế hoạch / thực tế, trọng số
ALTER TABLE project_progress ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0;
ALTER TABLE project_progress ADD COLUMN IF NOT EXISTS planned_start_date DATE;
ALTER TABLE project_progress ADD COLUMN IF NOT EXISTS planned_end_date DATE;
ALTER TABLE project_progress ADD COLUMN IF NOT EXISTS actual_start_date DATE;
ALTER TABLE project_progress ADD COLUMN IF NOT EXISTS actual_end_date DATE;
ALTER TABLE project_progress ADD COLUMN IF NOT EXISTS weight NUMERIC(8, 2) DEFAULT 1;
ALTER TABLE project_progress ADD COLUMN IF NOT EXISTS progress_percent NUMERIC(5, 2) DEFAULT 0;
ALTER TABLE project_progress ADD COLUMN IF NOT EXISTS note TEXT;

UPDATE project_progress SET sort_order = id WHERE sort_order IS NULL OR sort_order = 0;
UPDATE project_progress SET weight = 1 WHERE weight IS NULL OR weight <= 0;

CREATE INDEX IF NOT EXISTS idx_project_progress_sort ON project_progress (project_id, sort_order);

--changeset constructor:v1.1.7-02-task-workflow-columns
-- 2.22 + 2.23: Trọng số, ngày thực tế, thông tin duyệt / trả lại
ALTER TABLE task ADD COLUMN IF NOT EXISTS weight NUMERIC(8, 2) DEFAULT 1;
ALTER TABLE task ADD COLUMN IF NOT EXISTS actual_start_date DATE;
ALTER TABLE task ADD COLUMN IF NOT EXISTS actual_end_date DATE;
ALTER TABLE task ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP;
ALTER TABLE task ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;
ALTER TABLE task ADD COLUMN IF NOT EXISTS reviewed_by_id BIGINT;
ALTER TABLE task ADD COLUMN IF NOT EXISTS return_reason TEXT;
ALTER TABLE task ADD COLUMN IF NOT EXISTS overdue_notified_at TIMESTAMP;

UPDATE task SET weight = 1 WHERE weight IS NULL OR weight <= 0;
UPDATE task SET actual_end_date = COALESCE(end_date, CURRENT_DATE)
WHERE state = 'COMPLETED' AND actual_end_date IS NULL;

CREATE INDEX IF NOT EXISTS idx_task_state_end_date ON task (state, end_date);

--changeset constructor:v1.1.7-03-create-task-site-report
-- 2.25: Báo cáo hiện trường
CREATE TABLE IF NOT EXISTS task_site_report (
    id                 BIGSERIAL PRIMARY KEY,
    task_id            BIGINT        NOT NULL,
    project_id         BIGINT,
    report_phase       VARCHAR(20)   NOT NULL DEFAULT 'DURING',
    note               TEXT,
    images             TEXT,
    latitude           NUMERIC(10, 7),
    longitude          NUMERIC(10, 7),
    location_accuracy  NUMERIC(10, 2),
    location_address   VARCHAR(500),
    captured_at        TIMESTAMP,
    reporter_staff_id  BIGINT,
    reporter_name      VARCHAR(255),
    created_at         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(255),
    updated_at         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by         VARCHAR(255),
    deleted_at         TIMESTAMP,
    deleted_by         VARCHAR(255),
    CONSTRAINT fk_task_site_report_task FOREIGN KEY (task_id) REFERENCES task (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_task_site_report_task ON task_site_report (task_id);
CREATE INDEX IF NOT EXISTS idx_task_site_report_project ON task_site_report (project_id);

--changeset constructor:v1.1.7-04-create-task-comment
-- 2.27: Bình luận, @mention, đính kèm
CREATE TABLE IF NOT EXISTS task_comment (
    id                   BIGSERIAL PRIMARY KEY,
    task_id              BIGINT       NOT NULL,
    parent_id            BIGINT,
    content              TEXT         NOT NULL,
    mentioned_staff_ids  TEXT,
    attachments          TEXT,
    author_staff_id      BIGINT,
    author_name          VARCHAR(255),
    edited               BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    created_by           VARCHAR(255),
    updated_at           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR(255),
    deleted_at           TIMESTAMP,
    deleted_by           VARCHAR(255),
    CONSTRAINT fk_task_comment_task FOREIGN KEY (task_id) REFERENCES task (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_task_comment_task ON task_comment (task_id);

--changeset constructor:v1.1.7-05-create-notification
-- 2.26 + 2.27: Thông báo trễ hạn, duyệt việc, @mention
CREATE TABLE IF NOT EXISTS notification (
    id                  BIGSERIAL PRIMARY KEY,
    recipient_staff_id  BIGINT       NOT NULL,
    type                VARCHAR(50)  NOT NULL,
    title               VARCHAR(255) NOT NULL,
    content             TEXT,
    link                VARCHAR(500),
    ref_type            VARCHAR(50),
    ref_id              BIGINT,
    is_read             BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at             TIMESTAMP,
    created_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(255),
    updated_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(255),
    deleted_at          TIMESTAMP,
    deleted_by          VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_notification_recipient ON notification (recipient_staff_id, is_read, created_at DESC);
