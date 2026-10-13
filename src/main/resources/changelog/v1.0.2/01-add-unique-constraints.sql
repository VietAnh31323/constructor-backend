--liquibase formatted sql

-- ============================================================================
-- Version: v1.0.2
-- Description: Bổ sung ràng buộc UNIQUE cho username và mã code các bảng
-- Module: Database (Task ID 1.03)
-- ============================================================================

--changeset constructor:v1.0.2-01-add-unique-constraints
DO $$
BEGIN
    -- account.username
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_account_username') THEN
        ALTER TABLE account ADD CONSTRAINT uk_account_username UNIQUE (username);
    END IF;

    -- staff.code
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_staff_code') THEN
        ALTER TABLE staff ADD CONSTRAINT uk_staff_code UNIQUE (code);
    END IF;

    -- staff.email
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_staff_email') THEN
        ALTER TABLE staff ADD CONSTRAINT uk_staff_email UNIQUE (email);
    END IF;

    -- role.name
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_role_name') THEN
        ALTER TABLE role ADD CONSTRAINT uk_role_name UNIQUE (name);
    END IF;

    -- customer.code
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_customer_code') THEN
        ALTER TABLE customer ADD CONSTRAINT uk_customer_code UNIQUE (code);
    END IF;

    -- category.code
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_category_code') THEN
        ALTER TABLE category ADD CONSTRAINT uk_category_code UNIQUE (code);
    END IF;

    -- project.code
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_project_code') THEN
        ALTER TABLE project ADD CONSTRAINT uk_project_code UNIQUE (code);
    END IF;

    -- progress.code
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_progress_code') THEN
        ALTER TABLE progress ADD CONSTRAINT uk_progress_code UNIQUE (code);
    END IF;

    -- task.code
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_task_code') THEN
        ALTER TABLE task ADD CONSTRAINT uk_task_code UNIQUE (code);
    END IF;

    -- steel_category.code
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_steel_category_code') THEN
        ALTER TABLE steel_category ADD CONSTRAINT uk_steel_category_code UNIQUE (code);
    END IF;

    -- assembly.code
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_assembly_code') THEN
        ALTER TABLE assembly ADD CONSTRAINT uk_assembly_code UNIQUE (code);
    END IF;

    -- steel_project.code
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_steel_project_code') THEN
        ALTER TABLE steel_project ADD CONSTRAINT uk_steel_project_code UNIQUE (code);
    END IF;
END $$;
