--liquibase formatted sql

--changeset constructor:v1.0.0-01-create-extension
CREATE EXTENSION IF NOT EXISTS unaccent;

--changeset constructor:v1.0.0-02-create-table-account
CREATE TABLE IF NOT EXISTS account
(
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR,
    password    TEXT,
    first_name  VARCHAR,
    last_name   VARCHAR,
    dob         TIMESTAMP,
    status      VARCHAR,
    staff_count BIGINT,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR,
    updated_by  VARCHAR
);

--changeset constructor:v1.0.0-03-create-table-role
CREATE TABLE IF NOT EXISTS role
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR,
    description TEXT,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR,
    updated_by  VARCHAR
);

--changeset constructor:v1.0.0-04-create-table-account-role-map
CREATE TABLE IF NOT EXISTS account_role_map
(
    id         BIGSERIAL PRIMARY KEY,
    account_id BIGINT,
    role_id    BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR,
    updated_by VARCHAR
);

--changeset constructor:v1.0.0-05-create-table-token
CREATE TABLE IF NOT EXISTS token
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

--changeset constructor:v1.0.0-06-create-table-staff
CREATE TABLE IF NOT EXISTS staff
(
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR,
    email        VARCHAR,
    position     VARCHAR,
    name         VARCHAR,
    first_name   VARCHAR,
    last_name    VARCHAR,
    avatar       TEXT,
    birth_date   TIMESTAMP,
    address      VARCHAR,
    phone        VARCHAR,
    gender       VARCHAR,
    description  TEXT,
    gen_password VARCHAR,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP,
    created_by   VARCHAR,
    updated_by   VARCHAR
);

--changeset constructor:v1.0.0-07-create-table-account-staff-map
CREATE TABLE IF NOT EXISTS account_staff_map
(
    id         BIGSERIAL PRIMARY KEY,
    account_id BIGINT REFERENCES account (id),
    staff_id   BIGINT REFERENCES staff (id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR,
    updated_by VARCHAR
);

--changeset constructor:v1.0.0-08-create-table-password-reset
CREATE TABLE IF NOT EXISTS password_reset
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
