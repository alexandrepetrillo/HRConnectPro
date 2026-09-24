package com.hrconnect.payroll.domain.repository;

import com.hrconnect.payroll.domain.model.EmployeeSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour la projection locale des employés
 */
@Repository
public interface EmployeeSnapshotRepository extends JpaRepository<EmployeeSnapshot, Long> {

    /**
     * Recherche un snapshot par référence employé
     */
    Optional<EmployeeSnapshot> findByReference(String reference);

    /**
     * Vérifie si un événement a déjà été traité (idempotence)
     */
    boolean existsByLastEventId(String lastEventId);
}

