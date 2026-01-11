package com.hrconnect.employee.application.service;

import com.hrconnect.employee.domain.model.Employee;
import com.hrconnect.employee.domain.repository.EmployeeRepository;
import com.hrconnect.employee.infrastructure.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service métier pour la gestion des employés
 * Utilise le pattern Outbox pour garantir la cohérence transactionnelle
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final OutboxService outboxService;

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

    @Transactional(readOnly = true)
    public List<Employee> getEmployeesByManager(String managerId) {
        return employeeRepository.findByManagerId(managerId);
    }

    /**
     * Crée un nouvel employé et enregistre l'événement dans l'Outbox
     * Pattern Outbox : garantit que l'événement sera publié même en cas de panne
     */
    @Transactional
    public Employee createEmployee(Employee employee) {
        log.info("Creating employee: {}", employee.getReference());

        if (employeeRepository.existsByReference(employee.getReference())) {
            throw new IllegalArgumentException("Employee already exists: " + employee.getReference());
        }

        Employee saved = employeeRepository.save(employee);

        // Enregistrement dans l'Outbox (dans la même transaction)
        outboxService.saveEmployeeState(saved);

        log.info("Employee created and state saved to outbox: {}", saved.getReference());
        return saved;
    }

    /**
     * Met à jour un employé existant et enregistre l'événement dans l'Outbox
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

        // Enregistrement dans l'Outbox (dans la même transaction)
        outboxService.saveEmployeeState(updated);

        log.info("Employee updated and state saved to outbox: {}", updated.getReference());
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

