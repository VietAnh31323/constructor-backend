--liquibase formatted sql

-- ============================================================================
-- Version: v1.0.2
-- Description: Bổ sung ràng buộc UNIQUE cho username và mã code các bảng
-- Module: Database (Task ID 1.03)
-- ============================================================================

--changeset constructor:v1.0.2-01-add-unique-constraints splitStatements:false
DO $$
BEGIN
    -- Chuẩn hóa chuỗi rỗng thành NULL và xử lý trùng lặp dữ liệu cũ (nếu có) trước khi gắn UNIQUE
    UPDATE account SET username = NULL WHERE btrim(username) = '';
    WITH d AS (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY username ORDER BY id) AS rn
        FROM account WHERE username IS NOT NULL
    )
    UPDATE account a SET username = a.username || '_dup_' || a.id
    FROM d WHERE a.id = d.id AND d.rn > 1;

    -- Gộp role trùng tên (giữ bản ghi id nhỏ nhất)
    UPDATE role SET name = NULL WHERE btrim(name) = '';
    WITH canonical AS (
        SELECT name, MIN(id) AS keep_id
        FROM role
        WHERE name IS NOT NULL
        GROUP BY name
        HAVING COUNT(*) > 1
    )
    UPDATE account_role_map arm
    SET role_id = c.keep_id
    FROM role r
    JOIN canonical c ON r.name = c.name
    WHERE arm.role_id = r.id
      AND r.id <> c.keep_id;

    DELETE FROM role r
    USING (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY name ORDER BY id) AS rn
        FROM role WHERE name IS NOT NULL
    ) d
    WHERE r.id = d.id AND d.rn > 1;

    UPDATE staff SET code = NULL WHERE btrim(code) = '';
    WITH d AS (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY code ORDER BY id) AS rn
        FROM staff WHERE code IS NOT NULL
    )
    UPDATE staff t SET code = t.code || '_DUP_' || t.id
    FROM d WHERE t.id = d.id AND d.rn > 1;

    UPDATE staff SET email = NULL WHERE btrim(email) = '';
    WITH d AS (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY email ORDER BY id) AS rn
        FROM staff WHERE email IS NOT NULL
    )
    UPDATE staff t SET email = t.email || '_dup_' || t.id
    FROM d WHERE t.id = d.id AND d.rn > 1;

    UPDATE customer SET code = NULL WHERE btrim(code) = '';
    WITH d AS (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY code ORDER BY id) AS rn
        FROM customer WHERE code IS NOT NULL
    )
    UPDATE customer t SET code = t.code || '_DUP_' || t.id
    FROM d WHERE t.id = d.id AND d.rn > 1;

    UPDATE category SET code = NULL WHERE btrim(code) = '';
    WITH d AS (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY code ORDER BY id) AS rn
        FROM category WHERE code IS NOT NULL
    )
    UPDATE category t SET code = t.code || '_DUP_' || t.id
    FROM d WHERE t.id = d.id AND d.rn > 1;

    UPDATE project SET code = NULL WHERE btrim(code) = '';
    WITH d AS (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY code ORDER BY id) AS rn
        FROM project WHERE code IS NOT NULL
    )
    UPDATE project t SET code = t.code || '_DUP_' || t.id
    FROM d WHERE t.id = d.id AND d.rn > 1;

    UPDATE progress SET code = NULL WHERE btrim(code) = '';
    WITH d AS (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY code ORDER BY id) AS rn
        FROM progress WHERE code IS NOT NULL
    )
    UPDATE progress t SET code = t.code || '_DUP_' || t.id
    FROM d WHERE t.id = d.id AND d.rn > 1;

    UPDATE task SET code = NULL WHERE btrim(code) = '';
    WITH d AS (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY code ORDER BY id) AS rn
        FROM task WHERE code IS NOT NULL
    )
    UPDATE task t SET code = t.code || '_DUP_' || t.id
    FROM d WHERE t.id = d.id AND d.rn > 1;

    UPDATE steel_category SET code = NULL WHERE btrim(code) = '';
    WITH d AS (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY code ORDER BY id) AS rn
        FROM steel_category WHERE code IS NOT NULL
    )
    UPDATE steel_category t SET code = t.code || '_DUP_' || t.id
    FROM d WHERE t.id = d.id AND d.rn > 1;

    UPDATE assembly SET code = NULL WHERE btrim(code) = '';
    WITH d AS (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY code ORDER BY id) AS rn
        FROM assembly WHERE code IS NOT NULL
    )
    UPDATE assembly t SET code = t.code || '_DUP_' || t.id
    FROM d WHERE t.id = d.id AND d.rn > 1;

    UPDATE steel_project SET code = NULL WHERE btrim(code) = '';
    WITH d AS (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY code ORDER BY id) AS rn
        FROM steel_project WHERE code IS NOT NULL
    )
    UPDATE steel_project t SET code = t.code || '_DUP_' || t.id
    FROM d WHERE t.id = d.id AND d.rn > 1;

    -- account.username
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_account_username'
          AND connamespace = current_schema()::regnamespace
    ) THEN
        ALTER TABLE account ADD CONSTRAINT uk_account_username UNIQUE (username);
    END IF;

    -- staff.code
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_staff_code'
          AND connamespace = current_schema()::regnamespace
    ) THEN
        ALTER TABLE staff ADD CONSTRAINT uk_staff_code UNIQUE (code);
    END IF;

    -- staff.email
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_staff_email'
          AND connamespace = current_schema()::regnamespace
    ) THEN
        ALTER TABLE staff ADD CONSTRAINT uk_staff_email UNIQUE (email);
    END IF;

    -- role.name
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_role_name'
          AND connamespace = current_schema()::regnamespace
    ) THEN
        ALTER TABLE role ADD CONSTRAINT uk_role_name UNIQUE (name);
    END IF;

    -- customer.code
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_customer_code'
          AND connamespace = current_schema()::regnamespace
    ) THEN
        ALTER TABLE customer ADD CONSTRAINT uk_customer_code UNIQUE (code);
    END IF;

    -- category.code
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_category_code'
          AND connamespace = current_schema()::regnamespace
    ) THEN
        ALTER TABLE category ADD CONSTRAINT uk_category_code UNIQUE (code);
    END IF;

    -- project.code
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_project_code'
          AND connamespace = current_schema()::regnamespace
    ) THEN
        ALTER TABLE project ADD CONSTRAINT uk_project_code UNIQUE (code);
    END IF;

    -- progress.code
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_progress_code'
          AND connamespace = current_schema()::regnamespace
    ) THEN
        ALTER TABLE progress ADD CONSTRAINT uk_progress_code UNIQUE (code);
    END IF;

    -- task.code
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_task_code'
          AND connamespace = current_schema()::regnamespace
    ) THEN
        ALTER TABLE task ADD CONSTRAINT uk_task_code UNIQUE (code);
    END IF;

    -- steel_category.code
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_steel_category_code'
          AND connamespace = current_schema()::regnamespace
    ) THEN
        ALTER TABLE steel_category ADD CONSTRAINT uk_steel_category_code UNIQUE (code);
    END IF;

    -- assembly.code
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_assembly_code'
          AND connamespace = current_schema()::regnamespace
    ) THEN
        ALTER TABLE assembly ADD CONSTRAINT uk_assembly_code UNIQUE (code);
    END IF;

    -- steel_project.code
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_steel_project_code'
          AND connamespace = current_schema()::regnamespace
    ) THEN
        ALTER TABLE steel_project ADD CONSTRAINT uk_steel_project_code UNIQUE (code);
    END IF;
END $$;
