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
  private final EmployeeCreationService employeeCreationService;

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
   * ⚠️ ATTENTION : L'événement est publié APRÈS la transaction
   *
   * Séquence :
   * 1. Appel à EmployeeCreationService (transactionnel) → COMMIT
   * 2. Publication Kafka (hors transaction)
   *
   * Si la publication échoue, l'employé existe mais l'événement n'est pas publié !
   * Le consumer pourra se resynchroniser via un snapshot complet ultérieur.
   */
  public Employee createEmployee(Employee employee) {
    log.info("Creating employee: {}", employee.getReference());

    // Étape 1 : Créer l'employé dans une transaction (COMMIT à la fin)
    Employee saved = employeeCreationService.createEmployeeInTransaction(employee);
    log.info("✅ Transaction committed - Employee saved: {}", saved.getReference());

    // Étape 2 : Publier l'événement sur Kafka APRÈS le COMMIT
    try {
      log.info("Publishing employee.state event to Kafka...");
      employeeEventPublisher.publishEmployeeState(saved);
      log.info("✅ Employee state event published for employee: {}", saved.getReference());
    } catch (Exception e) {
      log.error("❌ Failed to publish employee state event for employee: {}", saved.getReference(), e);
      log.warn("⚠️  Employé créé en base, mais événement non publié. La resynchronisation sera nécessaire.");
      // L'employé existe déjà en base (transaction commitée)
      // L'événement pourra être republié via un mécanisme de resynchronisation
    }

    return saved;
  }


  /**
   * Met à jour un employé existant et publie l'événement
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

    // Publication de l'événement (sera exécuté après le commit de la transaction)
    try {
      employeeEventPublisher.publishEmployeeState(updated);
      log.info("Employee updated and event published: {}", updated.getReference());
    } catch (Exception e) {
      log.error("Failed to publish employee state event after update: {}", updated.getReference(), e);
      // La mise à jour est déjà committée, on log juste l'erreur
    }

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

