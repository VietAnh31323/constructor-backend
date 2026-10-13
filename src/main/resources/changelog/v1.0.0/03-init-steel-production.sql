--liquibase formatted sql

--changeset constructor:v1.0.0-19-create-table-steel-category
CREATE TABLE IF NOT EXISTS steel_category
(
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR,
    name        VARCHAR,
    images      TEXT,
    is_active   BOOLEAN,
    description TEXT,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR,
    updated_by  VARCHAR
);

--changeset constructor:v1.0.0-20-create-table-steel-category-line
CREATE TABLE IF NOT EXISTS steel_category_line
(
    id                BIGSERIAL PRIMARY KEY,
    steel_category_id BIGINT REFERENCES steel_category (id),
    param_name        VARCHAR,
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP,
    created_by        VARCHAR,
    updated_by        VARCHAR
);

--changeset constructor:v1.0.0-21-create-table-steel
CREATE TABLE IF NOT EXISTS steel
(
    id            BIGSERIAL PRIMARY KEY,
    images        TEXT,
    bar_code      BIGINT,
    bar_quantity  BIGINT,
    splice_length NUMERIC,
    bar_diameter  NUMERIC,
    length        NUMERIC,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    created_by    VARCHAR,
    updated_by    VARCHAR
);

--changeset constructor:v1.0.0-22-create-table-steel-line
CREATE TABLE IF NOT EXISTS steel_line
(
    id         BIGSERIAL PRIMARY KEY,
    steel_id   BIGINT NOT NULL REFERENCES steel (id),
    param_name VARCHAR,
    value      NUMERIC,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR,
    updated_by VARCHAR
);

--changeset constructor:v1.0.0-23-create-table-assembly
CREATE TABLE IF NOT EXISTS assembly
(
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR,
    name       VARCHAR,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR,
    updated_by VARCHAR
);

--changeset constructor:v1.0.0-24-create-table-steel-project
CREATE TABLE IF NOT EXISTS steel_project
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

--changeset constructor:v1.0.0-25-create-table-steel-project-assembly-map
CREATE TABLE IF NOT EXISTS steel_project_assembly_map
(
    id               BIGSERIAL PRIMARY KEY,
    steel_project_id BIGINT NOT NULL REFERENCES steel_project (id),
    assembly_name    VARCHAR,
    same_quantity    BIGINT,
    steel_ids        TEXT,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       VARCHAR,
    updated_by       VARCHAR
);

--changeset constructor:v1.0.0-26-create-table-summary-table
CREATE TABLE IF NOT EXISTS summary_table
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR,
    updated_by VARCHAR
);

--changeset constructor:v1.0.0-27-create-table-steel-project-table-map
CREATE TABLE IF NOT EXISTS steel_project_table_map
(
    id               BIGSERIAL PRIMARY KEY,
    steel_project_id BIGINT REFERENCES steel_project (id),
    summary_table_id BIGINT REFERENCES summary_table (id),
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       VARCHAR,
    updated_by       VARCHAR
);

--changeset constructor:v1.0.0-28-create-table-project-assembly-steel-map
CREATE TABLE IF NOT EXISTS project_assembly_steel_map
(
    id                            BIGSERIAL PRIMARY KEY,
    steel_project_assembly_map_id BIGINT NOT NULL REFERENCES steel_project_assembly_map (id),
    steel_id                      BIGINT NOT NULL REFERENCES steel (id),
    created_at                    TIMESTAMP,
    updated_at                    TIMESTAMP,
    created_by                    VARCHAR,
    updated_by                    VARCHAR
);

--changeset constructor:v1.0.0-29-create-table-summary-table-assembly-map
CREATE TABLE IF NOT EXISTS summary_table_assembly_map
(
    id               BIGSERIAL PRIMARY KEY,
    summary_table_id BIGINT REFERENCES summary_table (id),
    assembly_id      BIGINT REFERENCES assembly (id),
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       VARCHAR,
    updated_by       VARCHAR
);

--changeset constructor:v1.0.0-30-create-table-summary
CREATE TABLE IF NOT EXISTS summary
(
    id               BIGSERIAL PRIMARY KEY,
    steel_project_id BIGINT REFERENCES steel_project (id),
    diameter         NUMERIC,
    total_weight     NUMERIC,
    total_length     NUMERIC,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       VARCHAR,
    updated_by       VARCHAR
);
