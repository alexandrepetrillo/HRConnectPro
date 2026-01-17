-- V2__Create_failed_messages_table.sql
-- Table technique pour stocker les messages Kafka en erreur (DLQ)

CREATE TABLE payroll.failed_messages (
    id BIGSERIAL PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    message_key VARCHAR(255),
    payload TEXT NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_failed_messages_topic ON payroll.failed_messages(topic);
CREATE INDEX idx_failed_messages_created_at ON payroll.failed_messages(created_at);
