-- Migration pour créer la séquence pour l'ID des employés
-- Nécessaire car on passe de IDENTITY à SEQUENCE

-- Créer la séquence
CREATE SEQUENCE IF NOT EXISTS employee_id_seq START WITH 100;

-- Mettre à jour la séquence pour qu'elle démarre après les IDs existants
SELECT setval('employee_id_seq', COALESCE((SELECT MAX(id) FROM employee.employees), 0) + 1, false);

-- Note: La colonne id reste en BIGINT mais n'est plus en SERIAL (qui est équivalent à IDENTITY)
-- Hibernate utilisera maintenant la séquence pour générer les IDs
