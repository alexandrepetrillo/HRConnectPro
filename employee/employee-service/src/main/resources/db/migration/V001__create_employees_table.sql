-- Migration initiale : création de la table employees

CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    reference VARCHAR(50) NOT NULL UNIQUE,
    nom VARCHAR(200) NOT NULL,
    email VARCHAR(200) NOT NULL UNIQUE,
    telephone VARCHAR(20),
    role VARCHAR(100) NOT NULL,
    departement VARCHAR(100) NOT NULL,
    manager_id VARCHAR(50),
    type VARCHAR(50) NOT NULL,
    debut DATE NOT NULL,
    fin DATE,
    salaire_annuel_base DOUBLE PRECISION NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

-- Index pour améliorer les performances des requêtes
CREATE INDEX idx_employees_reference ON employees(reference);
CREATE INDEX idx_employees_departement ON employees(departement);
CREATE INDEX idx_employees_manager ON employees(manager_id);
CREATE INDEX idx_employees_email ON employees(email);

-- Commentaires pour documentation
COMMENT ON TABLE employees IS 'Table des employés';
COMMENT ON COLUMN employees.reference IS 'Référence unique de l''employé (ex: E001)';
COMMENT ON COLUMN employees.version IS 'Version pour optimistic locking';
COMMENT ON COLUMN employees.type IS 'Type de contrat (CDI, CDD, Stage, etc.)';
COMMENT ON COLUMN employees.debut IS 'Date de début du contrat';
COMMENT ON COLUMN employees.fin IS 'Date de fin du contrat (NULL pour CDI)';

