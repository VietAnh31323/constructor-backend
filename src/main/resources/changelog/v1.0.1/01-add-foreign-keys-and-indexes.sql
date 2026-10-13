--liquibase formatted sql

-- ============================================================================
-- Version: v1.0.1
-- Description: Bổ sung khóa ngoại (Foreign Keys) và chỉ mục (Indexes)
-- Module: Database (Task ID 1.02)
-- ============================================================================

--changeset constructor:v1.0.1-01-add-foreign-keys-project
DO $$
BEGIN
    -- FK project.creator_id -> staff(id)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_project_creator') THEN
        ALTER TABLE project
            ADD CONSTRAINT fk_project_creator
            FOREIGN KEY (creator_id) REFERENCES staff (id)
            ON DELETE SET NULL;
    END IF;

    -- FK project.manager_id -> staff(id)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_project_manager') THEN
        ALTER TABLE project
            ADD CONSTRAINT fk_project_manager
            FOREIGN KEY (manager_id) REFERENCES staff (id)
            ON DELETE SET NULL;
    END IF;

    -- FK project.supporter_id -> staff(id)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_project_supporter') THEN
        ALTER TABLE project
            ADD CONSTRAINT fk_project_supporter
            FOREIGN KEY (supporter_id) REFERENCES staff (id)
            ON DELETE SET NULL;
    END IF;
END $$;

--changeset constructor:v1.0.1-02-add-foreign-keys-auth
DO $$
BEGIN
    -- FK token.account_id -> account(id)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_token_account') THEN
        ALTER TABLE token
            ADD CONSTRAINT fk_token_account
            FOREIGN KEY (account_id) REFERENCES account (id)
            ON DELETE CASCADE;
    END IF;

    -- FK password_reset.account_id -> account(id)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_password_reset_account') THEN
        ALTER TABLE password_reset
            ADD CONSTRAINT fk_password_reset_account
            FOREIGN KEY (account_id) REFERENCES account (id)
            ON DELETE CASCADE;
    END IF;

    -- FK account_role_map.account_id -> account(id)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_account_role_map_account') THEN
        ALTER TABLE account_role_map
            ADD CONSTRAINT fk_account_role_map_account
            FOREIGN KEY (account_id) REFERENCES account (id)
            ON DELETE CASCADE;
    END IF;

    -- FK account_role_map.role_id -> role(id)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_account_role_map_role') THEN
        ALTER TABLE account_role_map
            ADD CONSTRAINT fk_account_role_map_role
            FOREIGN KEY (role_id) REFERENCES role (id)
            ON DELETE CASCADE;
    END IF;
END $$;

--changeset constructor:v1.0.1-03-add-indexes-code
CREATE INDEX IF NOT EXISTS idx_staff_code ON staff (code);
CREATE INDEX IF NOT EXISTS idx_customer_code ON customer (code);
CREATE INDEX IF NOT EXISTS idx_category_code ON category (code);
CREATE INDEX IF NOT EXISTS idx_project_code ON project (code);
CREATE INDEX IF NOT EXISTS idx_progress_code ON progress (code);
CREATE INDEX IF NOT EXISTS idx_task_code ON task (code);
CREATE INDEX IF NOT EXISTS idx_steel_category_code ON steel_category (code);
CREATE INDEX IF NOT EXISTS idx_assembly_code ON assembly (code);
CREATE INDEX IF NOT EXISTS idx_steel_project_code ON steel_project (code);

--changeset constructor:v1.0.1-04-add-indexes-state
CREATE INDEX IF NOT EXISTS idx_project_state ON project (state);
CREATE INDEX IF NOT EXISTS idx_project_progress_state ON project_progress (state);
CREATE INDEX IF NOT EXISTS idx_task_state ON task (state);

--changeset constructor:v1.0.1-05-add-indexes-project-id
CREATE INDEX IF NOT EXISTS idx_project_category_map_project_id ON project_category_map (project_id);
CREATE INDEX IF NOT EXISTS idx_project_line_project_id ON project_line (project_id);
CREATE INDEX IF NOT EXISTS idx_project_progress_project_id ON project_progress (project_id);
CREATE INDEX IF NOT EXISTS idx_summary_steel_project_id ON summary (steel_project_id);
CREATE INDEX IF NOT EXISTS idx_steel_project_assembly_map_steel_project_id ON steel_project_assembly_map (steel_project_id);
CREATE INDEX IF NOT EXISTS idx_steel_project_table_map_steel_project_id ON steel_project_table_map (steel_project_id);

--changeset constructor:v1.0.1-06-add-indexes-fk-lookup
CREATE INDEX IF NOT EXISTS idx_project_creator_id ON project (creator_id);
CREATE INDEX IF NOT EXISTS idx_project_manager_id ON project (manager_id);
CREATE INDEX IF NOT EXISTS idx_project_supporter_id ON project (supporter_id);
CREATE INDEX IF NOT EXISTS idx_token_account_id ON token (account_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_account_id ON password_reset (account_id);
CREATE INDEX IF NOT EXISTS idx_account_role_map_account_id ON account_role_map (account_id);
CREATE INDEX IF NOT EXISTS idx_account_role_map_role_id ON account_role_map (role_id);
CREATE INDEX IF NOT EXISTS idx_account_staff_map_account_id ON account_staff_map (account_id);
CREATE INDEX IF NOT EXISTS idx_account_staff_map_staff_id ON account_staff_map (staff_id);
CREATE INDEX IF NOT EXISTS idx_task_staff_map_task_id ON task_staff_map (task_id);
CREATE INDEX IF NOT EXISTS idx_task_staff_map_staff_id ON task_staff_map (staff_id);
