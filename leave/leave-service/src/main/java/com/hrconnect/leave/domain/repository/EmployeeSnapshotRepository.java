package com.hrconnect.leave.domain.repository;

import com.hrconnect.leave.domain.model.EmployeeSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeSnapshotRepository extends JpaRepository<EmployeeSnapshot, Long> {

    /**
     * Trouve un snapshot d'employé par sa référence
     */
    Optional<EmployeeSnapshot> findByReference(String reference);

    /**
     * Vérifie si un snapshot existe pour cette référence
     */
    boolean existsByReference(String reference);

    /**
     * Vérifie si un événement a déjà été traité (idempotence)
     */
    boolean existsByLastEventId(String eventId);
}
