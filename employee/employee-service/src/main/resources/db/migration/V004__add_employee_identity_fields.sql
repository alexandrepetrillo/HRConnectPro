-- V004__add_employee_identity_fields.sql
-- Ajout des champs d'identité pour la vérification du numéro de sécurité sociale

ALTER TABLE employees ADD COLUMN numero_securite_sociale VARCHAR(15);
ALTER TABLE employees ADD COLUMN date_naissance DATE;
