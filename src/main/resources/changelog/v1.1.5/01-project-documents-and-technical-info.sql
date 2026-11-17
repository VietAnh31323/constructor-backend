--liquibase formatted sql

--changeset constructor:v1.1.5-01-project-technical-info
ALTER TABLE project ADD COLUMN IF NOT EXISTS building_type VARCHAR(100);
ALTER TABLE project ADD COLUMN IF NOT EXISTS land_area NUMERIC(12, 2);
ALTER TABLE project ADD COLUMN IF NOT EXISTS construction_area NUMERIC(12, 2);
ALTER TABLE project ADD COLUMN IF NOT EXISTS total_floor_area NUMERIC(12, 2);
ALTER TABLE project ADD COLUMN IF NOT EXISTS number_of_floors INTEGER;
ALTER TABLE project ADD COLUMN IF NOT EXISTS construction_permit_no VARCHAR(100);
ALTER TABLE project ADD COLUMN IF NOT EXISTS construction_permit_date DATE;
ALTER TABLE project ADD COLUMN IF NOT EXISTS design_unit VARCHAR(255);

--changeset constructor:v1.1.5-02-create-project-document
CREATE TABLE IF NOT EXISTS project_document (
    id                 BIGSERIAL PRIMARY KEY,
    project_id         BIGINT        NOT NULL,
    folder             VARCHAR(50)   NOT NULL DEFAULT 'DRAWING',
    name               VARCHAR(255)  NOT NULL,
    file_name          VARCHAR(255),
    file_url           VARCHAR(1000) NOT NULL,
    file_type          VARCHAR(100),
    file_size          BIGINT,
    version            VARCHAR(30)   NOT NULL DEFAULT 'v1.0',
    uploaded_by_name   VARCHAR(255),
    uploaded_at        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    description        TEXT,
    created_at         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(255),
    updated_at         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by         VARCHAR(255),
    deleted_at         TIMESTAMP,
    deleted_by         VARCHAR(255),
    CONSTRAINT fk_project_document_project
        FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_project_document_project_id ON project_document (project_id);
CREATE INDEX IF NOT EXISTS idx_project_document_folder ON project_document (project_id, folder);
CREATE INDEX IF NOT EXISTS idx_project_document_uploaded_at ON project_document (uploaded_at);
