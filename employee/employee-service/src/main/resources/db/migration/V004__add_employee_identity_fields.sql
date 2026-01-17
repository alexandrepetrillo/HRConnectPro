-- V004__add_employee_identity_fields.sql
-- Ajout des champs d'identité pour la vérification du numéro de sécurité sociale

ALTER TABLE employees ADD COLUMN prenom VARCHAR(200) NOT NULL DEFAULT '';
ALTER TABLE employees ADD COLUMN numero_securite_sociale VARCHAR(15) UNIQUE;
ALTER TABLE employees ADD COLUMN date_naissance DATE;

-- Suppression des valeurs par défaut après migration
ALTER TABLE employees ALTER COLUMN prenom DROP DEFAULT;

-- Index pour la recherche par numéro de sécurité sociale
CREATE INDEX idx_employees_numero_secu ON employees(numero_securite_sociale);
