package com.hrconnect.employee.application.service;

import com.hrconnect.employee.domain.model.Employee;
import com.hrconnect.employee.domain.repository.EmployeeRepository;
import com.hrconnect.employee.infrastructure.kafka.EmployeeEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service métier pour la gestion des employés
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {

  private final EmployeeRepository employeeRepository;
  private final EmployeeEventPublisher employeeEventPublisher;

  @Transactional(readOnly = true)
  public List<Employee> getAllEmployees() {
    return employeeRepository.findAll();
  }

  @Transactional(readOnly = true)
  public Optional<Employee> getEmployeeById(String reference) {
    return employeeRepository.findByReference(reference);
  }

  @Transactional(readOnly = true)
  public List<Employee> getEmployeesByDepartement(String departement) {
    return employeeRepository.findByDepartement(departement);
  }

  /**
   * Crée un nouvel employé et publie l'événement sur Kafka
   *
   * ✅ L'événement est automatiquement publié APRÈS le commit de la transaction
   *    grâce au TransactionSynchronizationManager
   *
   * Séquence :
   * 1. Appel à EmployeeCreationService (transactionnel)
   * 2. Enregistrement de la publication Kafka pour après le commit
   * 3. COMMIT de la transaction
   * 4. Publication automatique sur Kafka
   */
  @Transactional
  public Employee createEmployee(Employee employee) {
    log.info("Creating employee: {}", employee.getReference());

    // Vérifier si l'employé existe déjà
    if (employeeRepository.existsByReference(employee.getReference())) {
      throw new IllegalArgumentException("Employee already exists: " + employee.getReference());
    }

    // Sauvegarder l'employé
    Employee saved = employeeRepository.save(employee);
    log.info("Employee saved in database: {}", saved.getReference());

    // Programmer la publication Kafka APRÈS le commit
    // Le TransactionSynchronizationManager dans le publisher se charge du reste
    employeeEventPublisher.publishEmployeeState(saved);

    return saved;
  }


  /**
   * Met à jour un employé existant et publie l'événement
   *
   * ✅ L'événement est automatiquement publié APRÈS le commit de la transaction
   */
  @Transactional
  public Employee updateEmployee(String reference, Employee employee) {
    log.info("Updating employee: {}", reference);

    Employee existing = employeeRepository.findByReference(reference)
      .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + reference));

    // Mise à jour des champs
    existing.setNom(employee.getNom());
    existing.setEmail(employee.getEmail());
    existing.setTelephone(employee.getTelephone());
    existing.setRole(employee.getRole());
    existing.setDepartement(employee.getDepartement());
    existing.setManagerId(employee.getManagerId());
    existing.setContrat(employee.getContrat());
    existing.setSalaireAnnuelBase(employee.getSalaireAnnuelBase());

    Employee updated = employeeRepository.save(existing);

    // Programmer la publication Kafka APRÈS le commit
    employeeEventPublisher.publishEmployeeState(updated);

    log.info("Employee updated, event will be published after commit: {}", updated.getReference());
    return updated;
  }

  /**
   * Supprime un employé (soft delete ou hard delete selon besoin métier)
   */
  @Transactional
  public void deleteEmployee(String reference) {
    log.info("Deleting employee: {}", reference);

    Employee employee = employeeRepository.findByReference(reference)
      .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + reference));

    employeeRepository.delete(employee);

    log.info("Employee deleted: {}", reference);
  }
}

