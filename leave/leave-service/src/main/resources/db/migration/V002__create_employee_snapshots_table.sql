-- Création de la table employee_snapshots (projection locale)
CREATE TABLE employee_snapshots (
    employee_id VARCHAR(255) PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(100),
    departement VARCHAR(100),
    manager_id VARCHAR(255),
    salaire_annuel_base DOUBLE PRECISION NOT NULL,
    last_updated TIMESTAMP NOT NULL
);

-- Index pour rechercher par email
CREATE INDEX idx_employee_snapshots_email ON employee_snapshots(email);

