-- Script d'initialisation de la base de données HRConnect
-- Crée les schémas pour chaque microservice

-- Schéma pour le microservice Employee
CREATE SCHEMA IF NOT EXISTS employee;

-- Accorder les privilèges au user hrconnect
GRANT ALL PRIVILEGES ON SCHEMA employee TO hrconnect;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA employee TO hrconnect;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA employee TO hrconnect;

-- Définir le search_path par défaut pour inclure le schéma employee
ALTER DATABASE hrconnect SET search_path TO employee, public;

-- Message de confirmation
DO $$
BEGIN
    RAISE NOTICE 'Base de données HRConnect initialisée avec succès';
    RAISE NOTICE 'Schéma employee créé';
END $$;

