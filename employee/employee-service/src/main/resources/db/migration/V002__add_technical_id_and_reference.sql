-- Migration: Ajout d'un ID technique auto-généré et transformation de l'ID en référence fonctionnelle
-- Date: 2025-11-22
-- Description:
--   - Ajoute une colonne 'id' technique (BIGSERIAL) comme nouvelle clé primaire
--   - Renomme l'ancienne colonne 'id' en 'reference' (identifiant fonctionnel unique)
--   - Maintient l'unicité de la référence

-- ATTENTION: Ce script nécessite une adaptation selon l'état de votre base de données
-- Si la table existe déjà avec des données, il faudra une migration progressive

-- Étape 1: Créer la nouvelle table avec la structure complète
CREATE TABLE IF NOT EXISTS employees_new (
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
    version BIGINT
);

-- Étape 2: Si migration depuis une table existante, copier les données
-- (Décommenter si nécessaire)
-- INSERT INTO employees_new (reference, nom, email, telephone, role, departement, manager_id, type, debut, fin, salaire_annuel_base, version)
-- SELECT id, nom, email, telephone, role, departement, manager_id, type, debut, fin, salaire_annuel_base, version
-- FROM employees;

-- Étape 3: Supprimer l'ancienne table et renommer la nouvelle
-- (Décommenter après avoir vérifié que les données sont bien migrées)
-- DROP TABLE IF EXISTS employees;
-- ALTER TABLE employees_new RENAME TO employees;

-- Étape 4: Créer les index pour les performances
CREATE INDEX IF NOT EXISTS idx_employees_reference ON employees(reference);
CREATE INDEX IF NOT EXISTS idx_employees_departement ON employees(departement);
CREATE INDEX IF NOT EXISTS idx_employees_manager_id ON employees(manager_id);
CREATE INDEX IF NOT EXISTS idx_employees_role ON employees(role);

-- Note: Avec spring.jpa.hibernate.ddl-auto=update,
-- Hibernate devrait gérer automatiquement la création/modification de la table.
-- Ce script est fourni pour documentation et pour les environnements de production
-- où Hibernate n'a pas les droits de modification de schéma.

