--liquibase formatted sql

--changeset constructor:v1.1.9-01-notification-email-and-preferences
-- 3.09: Theo dõi trạng thái gửi email thông báo và cấu hình nhận email của nhân sự
ALTER TABLE notification ADD COLUMN IF NOT EXISTS email_sent BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE notification ADD COLUMN IF NOT EXISTS email_sent_at TIMESTAMP;

ALTER TABLE staff ADD COLUMN IF NOT EXISTS receive_email_notifications BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE project_line ADD COLUMN IF NOT EXISTS last_due_alert_at TIMESTAMP;
