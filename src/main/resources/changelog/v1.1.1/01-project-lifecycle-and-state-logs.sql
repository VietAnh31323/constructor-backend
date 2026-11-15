--liquibase formatted sql

--changeset constructor:v1.1.1-01-extend-project-lifecycle-columns
ALTER TABLE project
    ADD COLUMN IF NOT EXISTS previous_state       VARCHAR(40),
    ADD COLUMN IF NOT EXISTS state_reason         TEXT,
    ADD COLUMN IF NOT EXISTS state_changed_at     TIMESTAMP,
    ADD COLUMN IF NOT EXISTS actual_handover_date DATE,
    ADD COLUMN IF NOT EXISTS warranty_end_date    DATE;

-- Chuyển đổi dữ liệu trạng thái cũ (NOT_STARTED, IN_PROGRESS, COMPLETED) sang vòng đời 9 bước mới
UPDATE project
SET state = CASE
                WHEN state = 'IN_PROGRESS' THEN 'IN_CONSTRUCTION'
                WHEN state = 'COMPLETED'   THEN 'CLOSED'
                WHEN state = 'NOT_STARTED' OR state IS NULL OR btrim(state) = '' THEN
                    CASE
                        WHEN sign_date IS NOT NULL OR COALESCE(contract_value, 0) > 0 THEN 'CONTRACT_SIGNED'
                        ELSE 'RECEIVED'
                    END
                ELSE state
            END,
    state_changed_at = COALESCE(state_changed_at, updated_at, created_at, NOW());

CREATE INDEX IF NOT EXISTS idx_project_state ON project (state);

--changeset constructor:v1.1.1-02-create-project-state-log
CREATE TABLE IF NOT EXISTS project_state_log (
    id                    BIGSERIAL PRIMARY KEY,
    project_id            BIGINT      NOT NULL,
    from_state            VARCHAR(40),
    to_state              VARCHAR(40) NOT NULL,
    reason                TEXT,
    changed_by_staff_id   BIGINT,
    changed_by_username   VARCHAR(255),
    created_at            TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(255),
    updated_at            TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_by            VARCHAR(255),
    deleted_at            TIMESTAMP,
    deleted_by            VARCHAR(255),
    CONSTRAINT fk_project_state_log_project
        FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE,
    CONSTRAINT fk_project_state_log_staff
        FOREIGN KEY (changed_by_staff_id) REFERENCES staff (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_project_state_log_project_time
    ON project_state_log (project_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_project_state_log_to_state
    ON project_state_log (to_state);

-- Khởi tạo dòng log đầu tiên cho các dự án hiện có trong hệ thống
INSERT INTO project_state_log (
    project_id,
    from_state,
    to_state,
    reason,
    changed_by_staff_id,
    changed_by_username,
    created_at,
    created_by,
    updated_at,
    updated_by
)
SELECT
    p.id,
    NULL,
    p.state,
    'Đồng bộ khởi tạo vòng đời dự án',
    p.creator_id,
    COALESCE(p.created_by, 'SYSTEM'),
    COALESCE(p.created_at, NOW()),
    COALESCE(p.created_by, 'SYSTEM'),
    NOW(),
    'SYSTEM'
FROM project p
WHERE p.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM project_state_log l
      WHERE l.project_id = p.id
  );
