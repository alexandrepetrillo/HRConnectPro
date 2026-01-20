package com.hrconnect.leave.domain.repository;

import com.hrconnect.leave.domain.model.EmployeeSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour la projection locale EmployeeSnapshot
 */
@Repository
public interface EmployeeSnapshotRepository extends JpaRepository<EmployeeSnapshot, String> {

    /**
     * Vérifie si un employé existe dans le snapshot local
     */
    boolean existsByEmployeeId(String employeeId);

    /**
     * Trouve un snapshot par employeeId
     */
    Optional<EmployeeSnapshot> findByEmployeeId(String employeeId);
}

