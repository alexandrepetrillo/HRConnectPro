package com.hrconnect.leave.domain.repository;

import com.hrconnect.leave.domain.model.Leave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour l'entité Leave
 */
@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {

    /**
     * Trouve tous les congés d'un employé
     */
    List<Leave> findByEmployeeId(String employeeId);

    /**
     * Trouve tous les congés par statut
     */
    List<Leave> findByStatut(com.hrconnect.leave.domain.model.LeaveStatus statut);
}

