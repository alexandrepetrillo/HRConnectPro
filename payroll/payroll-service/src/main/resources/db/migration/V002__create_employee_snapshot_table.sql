-- Table pour stocker le snapshot des employés (projection locale)
-- Alimentée par le consumer Kafka employee.state

CREATE TABLE employee_snapshot (
    id BIGSERIAL PRIMARY KEY,
    reference VARCHAR(50) NOT NULL UNIQUE,
    salaire_annuel_base DECIMAL(12, 2),
    last_event_id VARCHAR(100) NOT NULL,
    last_event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    event_version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index pour recherche par référence
CREATE INDEX idx_employee_snapshot_reference ON employee_snapshot(reference);

-- Index pour vérification d'idempotence
CREATE INDEX idx_employee_snapshot_last_event_id ON employee_snapshot(last_event_id);

