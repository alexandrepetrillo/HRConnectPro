-- Schema interview
CREATE SCHEMA IF NOT EXISTS interview;

-- Table des entretiens
CREATE TABLE interview.interviews (
    id BIGSERIAL PRIMARY KEY,
    reference VARCHAR(50) NOT NULL UNIQUE,
    employee_id VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    date_entretien DATE NOT NULL,
    feedback TEXT,
    augmentation_accordee DOUBLE PRECISION,
    statut VARCHAR(50) NOT NULL DEFAULT 'PLANIFIE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index
CREATE INDEX idx_interviews_employee_id ON interview.interviews(employee_id);
CREATE INDEX idx_interviews_statut ON interview.interviews(statut);
CREATE INDEX idx_interviews_date ON interview.interviews(date_entretien);
