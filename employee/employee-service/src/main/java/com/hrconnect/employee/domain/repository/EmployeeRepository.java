package com.hrconnect.employee.domain.repository;

import com.hrconnect.employee.domain.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour les employés
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByReference(String reference);

    boolean existsByReference(String reference);

    List<Employee> findByDepartement(String departement);

    List<Employee> findByManagerId(String managerId);

    List<Employee> findByRole(String role);
}

