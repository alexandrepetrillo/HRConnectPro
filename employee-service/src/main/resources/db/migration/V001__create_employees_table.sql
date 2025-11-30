-- Migration: Création de la table employees
-- Date: 2025-11-22
-- Description:
--   - Crée le schéma employee si nécessaire
--   - Crée la table employees avec un ID technique auto-généré (BIGSERIAL)
--   - Utilise une colonne 'reference' comme identifiant fonctionnel unique

-- Création du schéma
CREATE SCHEMA IF NOT EXISTS employee;

-- Définir le schéma de recherche
SET search_path TO employee;

CREATE TABLE IF NOT EXISTS employee.employees (
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

-- Index pour les performances
CREATE INDEX IF NOT EXISTS idx_employees_reference ON employee.employees(reference);
CREATE INDEX IF NOT EXISTS idx_employees_departement ON employee.employees(departement);
CREATE INDEX IF NOT EXISTS idx_employees_manager_id ON employee.employees(manager_id);
CREATE INDEX IF NOT EXISTS idx_employees_email ON employee.employees(email);
CREATE INDEX IF NOT EXISTS idx_employees_role ON employee.employees(role);

-- Commentaires pour documentation
COMMENT ON TABLE employee.employees IS 'Table des employés';
COMMENT ON COLUMN employee.employees.id IS 'ID technique auto-généré';
COMMENT ON COLUMN employee.employees.reference IS 'Référence unique de l''employé (ex: E001)';
COMMENT ON COLUMN employee.employees.version IS 'Version pour optimistic locking';
COMMENT ON COLUMN employee.employees.type IS 'Type de contrat (CDI, CDD, Stage, etc.)';
COMMENT ON COLUMN employee.employees.debut IS 'Date de début du contrat';
COMMENT ON COLUMN employee.employees.fin IS 'Date de fin du contrat (NULL pour CDI)';
