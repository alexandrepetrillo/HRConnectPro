package com.hrconnect.employee.domain.repository;

import com.hrconnect.employee.domain.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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

  List<Employee> findByRole(String role);

  List<Employee> findByContratDebutBeforeAndContratFinAfter(LocalDate d1, LocalDate d2);

  default List<Employee> findEmployeesActifs(LocalDate d1) {
    return findByContratDebutBeforeAndContratFinAfter(d1, d1);
  }

  @Query("SELECT e FROM Employee e WHERE e.contrat.debut <= :d1 AND (e.contrat.fin IS NULL OR e.contrat.fin >= :d1)")
  List<Employee> findActifs(LocalDate d1);

}

