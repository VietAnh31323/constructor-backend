--liquibase formatted sql

--changeset constructor:v1.1.3-01-create-contract-and-addendum
CREATE TABLE IF NOT EXISTS contract (
    id                   BIGSERIAL PRIMARY KEY,
    project_id           BIGINT         NOT NULL,
    contract_number      VARCHAR(100)   NOT NULL,
    name                 VARCHAR(255)   NOT NULL,
    contract_type        VARCHAR(50)    NOT NULL DEFAULT 'MAIN',
    status               VARCHAR(50)    NOT NULL DEFAULT 'SIGNED',
    sign_date            DATE,
    start_date           DATE,
    end_date             DATE,
    value_before_tax     NUMERIC(18, 2) NOT NULL DEFAULT 0,
    vat_percent          NUMERIC(5, 2)  NOT NULL DEFAULT 10,
    vat_amount           NUMERIC(18, 2) NOT NULL DEFAULT 0,
    value_after_tax      NUMERIC(18, 2) NOT NULL DEFAULT 0,
    advance_amount       NUMERIC(18, 2) DEFAULT 0,
    warranty_months      INTEGER        DEFAULT 12,
    warranty_percent     NUMERIC(5, 2)  DEFAULT 5,
    warranty_amount      NUMERIC(18, 2) DEFAULT 0,
    terms                TEXT,
    note                 TEXT,
    contract_files       TEXT,
    created_at           TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    created_by           VARCHAR(255),
    updated_at           TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR(255),
    deleted_at           TIMESTAMP,
    deleted_by           VARCHAR(255),
    CONSTRAINT fk_contract_project
        FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE,
    CONSTRAINT uq_contract_number
        UNIQUE (contract_number)
);

CREATE INDEX IF NOT EXISTS idx_contract_project_id ON contract (project_id);
CREATE INDEX IF NOT EXISTS idx_contract_number ON contract (contract_number);
CREATE INDEX IF NOT EXISTS idx_contract_status ON contract (status);

CREATE TABLE IF NOT EXISTS contract_addendum (
    id                   BIGSERIAL PRIMARY KEY,
    contract_id          BIGINT         NOT NULL,
    project_id           BIGINT         NOT NULL,
    addendum_number      VARCHAR(100)   NOT NULL,
    name                 VARCHAR(255)   NOT NULL,
    adjustment_type      VARCHAR(50)    NOT NULL DEFAULT 'INCREASE',
    sign_date            DATE,
    effective_date       DATE,
    adjusted_days        INTEGER        DEFAULT 0,
    new_end_date         DATE,
    value_before_tax     NUMERIC(18, 2) NOT NULL DEFAULT 0,
    vat_percent          NUMERIC(5, 2)  NOT NULL DEFAULT 10,
    vat_amount           NUMERIC(18, 2) NOT NULL DEFAULT 0,
    value_after_tax      NUMERIC(18, 2) NOT NULL DEFAULT 0,
    reason               TEXT,
    content              TEXT,
    note                 TEXT,
    addendum_files       TEXT,
    created_at           TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    created_by           VARCHAR(255),
    updated_at           TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR(255),
    deleted_at           TIMESTAMP,
    deleted_by           VARCHAR(255),
    CONSTRAINT fk_contract_addendum_contract
        FOREIGN KEY (contract_id) REFERENCES contract (id) ON DELETE CASCADE,
    CONSTRAINT fk_contract_addendum_project
        FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE,
    CONSTRAINT uq_addendum_number
        UNIQUE (contract_id, addendum_number)
);

CREATE INDEX IF NOT EXISTS idx_contract_addendum_contract_id ON contract_addendum (contract_id);
CREATE INDEX IF NOT EXISTS idx_contract_addendum_project_id ON contract_addendum (project_id);

-- Migrate dữ liệu Hợp đồng ban đầu từ các dự án đã có contract_value
INSERT INTO contract (
    project_id,
    contract_number,
    name,
    contract_type,
    status,
    sign_date,
    end_date,
    value_before_tax,
    vat_percent,
    vat_amount,
    value_after_tax,
    advance_amount,
    contract_files,
    created_at,
    created_by,
    updated_at,
    updated_by
)
SELECT 
    p.id,
    COALESCE(p.code, 'HD-' || p.id),
    'Hợp đồng thi công ' || COALESCE(p.name, ''),
    'MAIN',
    'SIGNED',
    COALESCE(p.sign_date, CURRENT_DATE),
    p.delivery_date,
    ROUND(COALESCE(p.contract_value, 0) / 1.1, 2),
    10.0,
    COALESCE(p.contract_value, 0) - ROUND(COALESCE(p.contract_value, 0) / 1.1, 2),
    COALESCE(p.contract_value, 0),
    COALESCE(p.contract_advance, 0),
    p.contract_files,
    NOW(),
    'SYSTEM',
    NOW(),
    'SYSTEM'
FROM project p
WHERE (COALESCE(p.contract_value, 0) > 0 OR p.sign_date IS NOT NULL)
  AND NOT EXISTS (
      SELECT 1 FROM contract c WHERE c.project_id = p.id
  );
