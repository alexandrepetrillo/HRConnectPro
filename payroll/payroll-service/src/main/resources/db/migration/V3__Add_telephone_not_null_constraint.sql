-- V3__Add_telephone_not_null_constraint.sql
-- ⚠️ BUG VOLONTAIRE pour démonstration DLQ
--
-- Un développeur a ajouté cette contrainte en pensant que le téléphone
-- était toujours renseigné. Ça marchait... jusqu'à ce qu'un employé soit créé sans téléphone !

-- D'abord, mettre une valeur par défaut pour les lignes existantes sans téléphone
UPDATE payroll.employee_snapshots
SET telephone = 'NON_RENSEIGNE'
WHERE telephone IS NULL;

-- Puis ajouter la contrainte NOT NULL (le bug !)
ALTER TABLE payroll.employee_snapshots
ALTER COLUMN telephone SET NOT NULL;
