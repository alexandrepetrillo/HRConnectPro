-- Table des compteurs de congés
-- Contient les données PROPRES à Leave-Service (pas les données d'autres microservices)

CREATE TABLE IF NOT EXISTS leave_counters (
    employee_id VARCHAR(255) PRIMARY KEY,
    soldecp INTEGER NOT NULL DEFAULT 25,
    soldertt INTEGER NOT NULL DEFAULT 12,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE leave_counters IS 'Compteurs de congés par employé - données propres à Leave-Service';
COMMENT ON COLUMN leave_counters.employee_id IS 'Identifiant de l''employé (clé primaire)';
COMMENT ON COLUMN leave_counters.soldecp IS 'Solde de congés payés en jours';
COMMENT ON COLUMN leave_counters.soldertt IS 'Solde de RTT en jours';
COMMENT ON COLUMN leave_counters.last_updated IS 'Date de dernière mise à jour';
