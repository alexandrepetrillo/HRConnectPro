package com.hrconnect.leave.domain.repository;

import com.hrconnect.leave.domain.model.EmployeeLeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour la gestion des compteurs de congés des employés
 */
@Repository
public interface EmployeeLeaveBalanceRepository extends JpaRepository<EmployeeLeaveBalance, Long> {

    Optional<EmployeeLeaveBalance> findByEmployeeId(String employeeId);

    boolean existsByEmployeeId(String employeeId);
}
