package com.hrconnect.employee.application.service;

import com.hrconnect.employee.domain.model.Employee;
import com.hrconnect.employee.domain.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service transactionnel pour la création d'employés
 *
 * Ce service est appelé par EmployeeService et gère uniquement
 * la partie transactionnelle de la création d'un employé.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeCreationService {

  private final EmployeeRepository employeeRepository;

  /**
   * Crée un employé dans une transaction
   *
   * @param employee L'employé à créer
   * @return L'employé créé
   * @throws IllegalArgumentException si l'employé existe déjà
   */
  @Transactional
  public Employee createEmployeeInTransaction(Employee employee) {
    log.info("Creating employee in transaction: {}", employee.getReference());

    if (employeeRepository.existsByReference(employee.getReference())) {
      throw new IllegalArgumentException("Employee already exists: " + employee.getReference());
    }

    Employee saved = employeeRepository.save(employee);
    log.info("Employee saved in database: {}", saved.getReference());

    return saved;
  }
}
