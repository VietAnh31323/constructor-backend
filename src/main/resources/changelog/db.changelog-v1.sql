CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE account
(
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR,
    password   TEXT,
    first_name VARCHAR,
    last_name  VARCHAR,
    dob        TIMESTAMP,
    status     VARCHAR,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR,
    updated_by VARCHAR
);

CREATE TABLE role
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR,
    description TEXT,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR,
    updated_by  VARCHAR
);

CREATE TABLE account_role_map
(
    id         BIGSERIAL PRIMARY KEY,
    account_id BIGINT,
    role_id    BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR,
    updated_by VARCHAR
);

CREATE TABLE token
(
    id          BIGSERIAL PRIMARY KEY,
    account_id  BIGINT,
    token       TEXT,
    type        VARCHAR,
    expiry_date TIMESTAMP,
    revoked     BOOLEAN,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR,
    updated_by  VARCHAR
);

CREATE TABLE staff
(
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR,
    email       VARCHAR,
    name        VARCHAR,
    first_name  VARCHAR,
    last_name   VARCHAR,
    avatar      TEXT,
    birth_date  TIMESTAMP,
    address     VARCHAR,
    phone       VARCHAR,
    gender      VARCHAR,
    description TEXT,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR,
    updated_by  VARCHAR
);

CREATE TABLE account_staff_map
(
    id         BIGSERIAL PRIMARY KEY,
    account_id BIGINT REFERENCES account (id),
    staff_id   BIGINT REFERENCES staff (id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR,
    updated_by VARCHAR
);

CREATE TABLE customer
(
    id             BIGSERIAL PRIMARY KEY,
    code           VARCHAR,
    name           VARCHAR,
    phone          VARCHAR,
    email          VARCHAR,
    contact_status VARCHAR,
    is_potential   BOOLEAN,
    description    TEXT,
    note           TEXT,
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP,
    created_by     VARCHAR,
    updated_by     VARCHAR
);

CREATE TABLE category
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

CREATE TABLE project
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

CREATE TABLE project_category_map
(
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT REFERENCES project (id),
    category_id BIGINT REFERENCES category (id),
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR,
    updated_by  VARCHAR
);

CREATE TABLE project_line
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

CREATE TABLE progress
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

CREATE TABLE project_progress_map
(
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT REFERENCES project (id),
    progress_id BIGINT REFERENCES progress (id),
    state       VARCHAR,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR,
    updated_by  VARCHAR
);

CREATE TABLE task
(
    id               BIGSERIAL PRIMARY KEY,
    code             VARCHAR,
    name             VARCHAR,
    start_date       TIMESTAMP,
    end_date         TIMESTAMP,
    progress_percent FLOAT,
    reviewer_id      BIGINT,
    parent_id        BIGINT,
    state            VARCHAR,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       VARCHAR,
    updated_by       VARCHAR
);

CREATE TABLE task_staff_map
(
    id         BIGSERIAL PRIMARY KEY,
    task_id    BIGINT,
    staff_id   BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR,
    updated_by VARCHAR
);

CREATE TABLE project_progress_task_map
(
    id                      BIGSERIAL PRIMARY KEY,
    project_progress_map_id BIGINT REFERENCES project_progress_map (id),
    task_id                 BIGINT,
    created_at              TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              VARCHAR,
    updated_by              VARCHAR
);

CREATE TABLE steel_project
(
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR,
    name          VARCHAR,
    owner         VARCHAR,
    address       VARCHAR,
    sign_date     TIMESTAMP,
    delivery_date TIMESTAMP,
    description   TEXT,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    created_by    VARCHAR,
    updated_by    VARCHAR
);

CREATE TABLE summary_table
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR,
    updated_by VARCHAR
);

CREATE TABLE steel_project_table_map
(
    id               BIGSERIAL PRIMARY KEY,
    steel_project_id BIGINT REFERENCES steel_project (id),
    summary_table_id BIGINT REFERENCES summary_table (id),
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       VARCHAR,
    updated_by       VARCHAR
);

CREATE TABLE assembly
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR,
    quantity   NUMERIC,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR,
    updated_by VARCHAR
);

CREATE TABLE summary_table_assembly_map
(
    id               BIGSERIAL PRIMARY KEY,
    summary_table_id BIGINT references summary_table (id),
    assembly_id      BIGINT references assembly (id),
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       VARCHAR,
    updated_by       VARCHAR
);

CREATE TABLE steel_category
(
    id         BIGSERIAL PRIMARY KEY,
    url        VARCHAR,
    hasL1      BOOLEAN,
    hasL2      BOOLEAN,
    hasL3      BOOLEAN,
    hasM       BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR,
    updated_by VARCHAR
);

CREATE TABLE steel
(
    id                BIGSERIAL PRIMARY KEY,
    item_no           NUMERIC,
    steel_category_id BIGINT REFERENCES steel_category (id),
    diameter          NUMERIC,
    length            NUMERIC,
    quantity          NUMERIC,
    totalQuantity     NUMERIC,
    totalLength       NUMERIC,
    totalWeight       NUMERIC,
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP,
    created_by        VARCHAR,
    updated_by        VARCHAR
);

CREATE TABLE summary
(
    id               BIGSERIAL PRIMARY KEY,
    steel_project_id BIGINT references steel_project (id),
    diameter         NUMERIC,
    totalWeight      NUMERIC,
    totalLength      NUMERIC,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       VARCHAR,
    updated_by       VARCHAR
);

alter table customer
    add column address varchar;

CREATE TABLE password_reset
(
    id         BIGSERIAL PRIMARY KEY,
    account_id BIGINT    NOT NULL,
    otp_hash   TEXT      NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used       BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR,
    updated_by VARCHAR
);

alter table account
    add column staff_count BIGINT;











