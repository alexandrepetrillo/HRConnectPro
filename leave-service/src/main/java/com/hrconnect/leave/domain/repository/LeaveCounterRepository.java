package com.hrconnect.leave.domain.repository;

import com.hrconnect.leave.domain.model.LeaveCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour les compteurs de congés.
 */
@Repository
public interface LeaveCounterRepository extends JpaRepository<LeaveCounter, String> {

    /**
     * Trouve un compteur par ID employé.
     */
    Optional<LeaveCounter> findByEmployeeId(String employeeId);

    /**
     * Vérifie si un compteur existe pour un employé.
     */
    boolean existsByEmployeeId(String employeeId);
}

