--liquibase formatted sql

--changeset constructor:v1.0.0-09-create-table-customer
CREATE TABLE IF NOT EXISTS customer
(
    id             BIGSERIAL PRIMARY KEY,
    code           VARCHAR,
    name           VARCHAR,
    phone          VARCHAR,
    email          VARCHAR,
    address        VARCHAR,
    contact_status VARCHAR,
    is_potential   BOOLEAN,
    description    TEXT,
    note           TEXT,
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP,
    created_by     VARCHAR,
    updated_by     VARCHAR
);

--changeset constructor:v1.0.0-10-create-table-category
CREATE TABLE IF NOT EXISTS category
(
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR,
    name        VARCHAR,
    description TEXT,
    is_active   BOOLEAN,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR,
    updated_by  VARCHAR
);

--changeset constructor:v1.0.0-11-create-table-project
CREATE TABLE IF NOT EXISTS project
(
    id               BIGSERIAL PRIMARY KEY,
    code             VARCHAR,
    name             VARCHAR,
    owner            VARCHAR,
    address          VARCHAR,
    contract_value   NUMERIC,
    contract_advance NUMERIC,
    remaining_amount NUMERIC,
    sign_date        TIMESTAMP,
    delivery_date    TIMESTAMP,
    creator_id       BIGINT,
    manager_id       BIGINT,
    supporter_id     BIGINT,
    progress_percent NUMERIC,
    description      TEXT,
    note             TEXT,
    state            VARCHAR,
    contract_files   TEXT,
    sample_images    TEXT,
    project_images   TEXT,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       VARCHAR,
    updated_by       VARCHAR
);

--changeset constructor:v1.0.0-12-create-table-project-category-map
CREATE TABLE IF NOT EXISTS project_category_map
(
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT REFERENCES project (id),
    category_id BIGINT REFERENCES category (id),
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR,
    updated_by  VARCHAR
);

--changeset constructor:v1.0.0-13-create-table-project-line
CREATE TABLE IF NOT EXISTS project_line
(
    id             BIGSERIAL PRIMARY KEY,
    project_id     BIGINT REFERENCES project (id),
    payment_date   TIMESTAMP,
    payment_no     BIGINT,
    payment_amount NUMERIC,
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP,
    created_by     VARCHAR,
    updated_by     VARCHAR
);

--changeset constructor:v1.0.0-14-create-table-progress
CREATE TABLE IF NOT EXISTS progress
(
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR,
    name        VARCHAR,
    description TEXT,
    is_active   BOOLEAN,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR,
    updated_by  VARCHAR
);

--changeset constructor:v1.0.0-15-create-table-project-progress
CREATE TABLE IF NOT EXISTS project_progress
(
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT NOT NULL REFERENCES project (id),
    progress_id BIGINT NOT NULL REFERENCES progress (id),
    state       VARCHAR,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR,
    updated_by  VARCHAR
);

--changeset constructor:v1.0.0-16-create-table-task
CREATE TABLE IF NOT EXISTS task
(
    id               BIGSERIAL PRIMARY KEY,
    code             VARCHAR,
    name             VARCHAR,
    start_date       TIMESTAMP,
    end_date         TIMESTAMP,
    progress_percent NUMERIC,
    reviewer_id      BIGINT REFERENCES staff (id),
    parent_id        BIGINT REFERENCES task (id),
    priority_level   VARCHAR,
    state            VARCHAR,
    description      TEXT,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       VARCHAR,
    updated_by       VARCHAR
);

--changeset constructor:v1.0.0-17-create-table-task-staff-map
CREATE TABLE IF NOT EXISTS task_staff_map
(
    id         BIGSERIAL PRIMARY KEY,
    task_id    BIGINT REFERENCES task (id),
    staff_id   BIGINT REFERENCES staff (id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR,
    updated_by VARCHAR
);

--changeset constructor:v1.0.0-18-create-table-project-progress-task-map
CREATE TABLE IF NOT EXISTS project_progress_task_map
(
    id                  BIGSERIAL PRIMARY KEY,
    project_progress_id BIGINT NOT NULL REFERENCES project_progress (id),
    task_id             BIGINT NOT NULL REFERENCES task (id),
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR,
    updated_by          VARCHAR
);
