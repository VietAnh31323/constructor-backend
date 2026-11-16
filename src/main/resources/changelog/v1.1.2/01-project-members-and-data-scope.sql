--liquibase formatted sql

--changeset constructor:v1.1.2-01-create-project-member
CREATE TABLE IF NOT EXISTS project_member (
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT       NOT NULL,
    staff_id    BIGINT       NOT NULL,
    member_role VARCHAR(50)  NOT NULL,
    is_primary  BOOLEAN      DEFAULT FALSE,
    joined_date DATE,
    note        TEXT,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(255),
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by  VARCHAR(255),
    deleted_at  TIMESTAMP,
    deleted_by  VARCHAR(255),
    CONSTRAINT fk_project_member_project
        FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE,
    CONSTRAINT fk_project_member_staff
        FOREIGN KEY (staff_id) REFERENCES staff (id) ON DELETE CASCADE,
    CONSTRAINT uq_project_member_staff_role
        UNIQUE (project_id, staff_id, member_role)
);

CREATE INDEX IF NOT EXISTS idx_project_member_project_id ON project_member (project_id);
CREATE INDEX IF NOT EXISTS idx_project_member_staff_id ON project_member (staff_id);
CREATE INDEX IF NOT EXISTS idx_project_member_role ON project_member (member_role);

-- Migrate dữ liệu cũ từ creator_id, manager_id, supporter_id sang bảng project_member
INSERT INTO project_member (project_id, staff_id, member_role, is_primary, created_at, created_by, updated_at, updated_by)
SELECT p.id, p.manager_id, 'PROJECT_MANAGER', TRUE, NOW(), 'SYSTEM', NOW(), 'SYSTEM'
FROM project p
WHERE p.manager_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM project_member pm 
      WHERE pm.project_id = p.id AND pm.staff_id = p.manager_id AND pm.member_role = 'PROJECT_MANAGER'
  );

INSERT INTO project_member (project_id, staff_id, member_role, is_primary, created_at, created_by, updated_at, updated_by)
SELECT p.id, p.creator_id, 'CREATOR', FALSE, NOW(), 'SYSTEM', NOW(), 'SYSTEM'
FROM project p
WHERE p.creator_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM project_member pm 
      WHERE pm.project_id = p.id AND pm.staff_id = p.creator_id AND pm.member_role = 'CREATOR'
  );

INSERT INTO project_member (project_id, staff_id, member_role, is_primary, created_at, created_by, updated_at, updated_by)
SELECT p.id, p.supporterId, 'SUPPORTER', FALSE, NOW(), 'SYSTEM', NOW(), 'SYSTEM'
FROM (
    SELECT id, supporter_id AS supporterId FROM project WHERE supporter_id IS NOT NULL
) p
WHERE NOT EXISTS (
    SELECT 1 FROM project_member pm 
    WHERE pm.project_id = p.id AND pm.staff_id = p.supporterId AND pm.member_role = 'SUPPORTER'
);
