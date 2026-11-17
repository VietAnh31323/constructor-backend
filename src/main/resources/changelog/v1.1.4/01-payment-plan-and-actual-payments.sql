--liquibase formatted sql

--changeset constructor:v1.1.4-01-payment-plan-columns-on-project-line
ALTER TABLE project_line ADD COLUMN IF NOT EXISTS percentage NUMERIC(5, 2) DEFAULT 0;
ALTER TABLE project_line ADD COLUMN IF NOT EXISTS condition_milestone VARCHAR(500);
ALTER TABLE project_line ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'PLANNED';
ALTER TABLE project_line ADD COLUMN IF NOT EXISTS note TEXT;

UPDATE project_line pl
SET percentage = ROUND((pl.payment_amount / p.contract_value) * 100.0, 2)
FROM project p
WHERE pl.project_id = p.id
  AND p.contract_value IS NOT NULL
  AND p.contract_value > 0
  AND pl.payment_amount IS NOT NULL
  AND (pl.percentage IS NULL OR pl.percentage = 0);

--changeset constructor:v1.1.4-02-create-project-payment
CREATE TABLE IF NOT EXISTS project_payment (
    id               BIGSERIAL PRIMARY KEY,
    project_id       BIGINT         NOT NULL,
    project_line_id  BIGINT,
    receipt_code     VARCHAR(50),
    payment_date     DATE           NOT NULL,
    amount           NUMERIC(18, 2) NOT NULL DEFAULT 0,
    payment_method   VARCHAR(30)    NOT NULL DEFAULT 'BANK_TRANSFER',
    status           VARCHAR(30)    NOT NULL DEFAULT 'CONFIRMED',
    payer_name       VARCHAR(255),
    attachments      TEXT,
    note             TEXT,
    created_at       TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(255),
    updated_at       TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_by       VARCHAR(255),
    deleted_at       TIMESTAMP,
    deleted_by       VARCHAR(255),
    CONSTRAINT fk_project_payment_project
        FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE,
    CONSTRAINT fk_project_payment_line
        FOREIGN KEY (project_line_id) REFERENCES project_line (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_project_payment_project_id ON project_payment (project_id);
CREATE INDEX IF NOT EXISTS idx_project_payment_line_id ON project_payment (project_line_id);
CREATE INDEX IF NOT EXISTS idx_project_payment_status ON project_payment (status);
CREATE INDEX IF NOT EXISTS idx_project_payment_date ON project_payment (payment_date);
