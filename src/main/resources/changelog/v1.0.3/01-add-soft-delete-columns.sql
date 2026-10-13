--liquibase formatted sql

-- ============================================================================
-- Version: v1.0.3
-- Description: Bổ sung deleted_at, deleted_by phục vụ xóa mềm (Soft Delete)
-- Module: Database (Task ID 1.04)
-- ============================================================================

--changeset constructor:v1.0.3-01-add-soft-delete-columns
ALTER TABLE account ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE role ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE account_role_map ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE token ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE staff ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE account_staff_map ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE customer ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE category ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE project ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE project_category_map ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE project_line ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE progress ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE project_progress ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE task ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE task_staff_map ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE project_progress_task_map ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE steel_category ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE steel_category_line ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE steel ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE steel_line ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE assembly ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE steel_project ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE steel_project_assembly_map ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE summary_table ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE steel_project_table_map ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE project_assembly_steel_map ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE summary_table_assembly_map ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE summary ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;
ALTER TABLE password_reset ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR;

--changeset constructor:v1.0.3-02-add-indexes-soft-delete
CREATE INDEX IF NOT EXISTS idx_account_deleted_at ON account (deleted_at);
CREATE INDEX IF NOT EXISTS idx_staff_deleted_at ON staff (deleted_at);
CREATE INDEX IF NOT EXISTS idx_customer_deleted_at ON customer (deleted_at);
CREATE INDEX IF NOT EXISTS idx_category_deleted_at ON category (deleted_at);
CREATE INDEX IF NOT EXISTS idx_project_deleted_at ON project (deleted_at);
CREATE INDEX IF NOT EXISTS idx_task_deleted_at ON task (deleted_at);
CREATE INDEX IF NOT EXISTS idx_steel_project_deleted_at ON steel_project (deleted_at);
CREATE INDEX IF NOT EXISTS idx_assembly_deleted_at ON assembly (deleted_at);
