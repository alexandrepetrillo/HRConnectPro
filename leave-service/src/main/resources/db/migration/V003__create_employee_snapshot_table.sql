-- Migration: Création de la table employee_snapshot
-- Cette table stocke une projection locale des employés synchronisée via Kafka

CREATE TABLE IF NOT EXISTS leave.employee_snapshot
(
    id                      BIGSERIAL PRIMARY KEY,
    reference               VARCHAR(50) NOT NULL UNIQUE,
    nom                     VARCHAR(255) NOT NULL,
    email                   VARCHAR(255) NOT NULL,
    telephone               VARCHAR(50),
    role                    VARCHAR(100),
    departement             VARCHAR(100),
    manager_id              VARCHAR(50),
    contrat_type            VARCHAR(50),
    contrat_debut           DATE,
    contrat_fin             DATE,
    salaire_annuel_base     DOUBLE PRECISION NOT NULL,
    last_event_id           VARCHAR(255) NOT NULL,
    last_event_timestamp    TIMESTAMP NOT NULL,
    event_version           BIGINT NOT NULL,
    updated_at              TIMESTAMP NOT NULL,
    CONSTRAINT uk_employee_snapshot_reference UNIQUE (reference),
    CONSTRAINT uk_employee_snapshot_event_id UNIQUE (last_event_id)
);

-- Index pour améliorer les performances de recherche
CREATE INDEX IF NOT EXISTS idx_employee_snapshot_reference ON leave.employee_snapshot (reference);
CREATE INDEX IF NOT EXISTS idx_employee_snapshot_departement ON leave.employee_snapshot (departement);
CREATE INDEX IF NOT EXISTS idx_employee_snapshot_last_event_id ON leave.employee_snapshot (last_event_id);

-- Commentaires
COMMENT ON TABLE leave.employee_snapshot IS 'Projection locale des employés synchronisée via Kafka depuis employee-service';
COMMENT ON COLUMN leave.employee_snapshot.reference IS 'Référence unique de l''employé (clé fonctionnelle)';
COMMENT ON COLUMN leave.employee_snapshot.last_event_id IS 'ID du dernier événement traité (pour idempotence)';
COMMENT ON COLUMN leave.employee_snapshot.last_event_timestamp IS 'Timestamp du dernier événement traité';
COMMENT ON COLUMN leave.employee_snapshot.event_version IS 'Version de l''événement';
COMMENT ON COLUMN leave.employee_snapshot.updated_at IS 'Date de dernière mise à jour du snapshot';
