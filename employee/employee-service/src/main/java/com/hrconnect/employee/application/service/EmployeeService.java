package com.hrconnect.employee.application.service;

import com.hrconnect.employee.domain.model.Employee;
import com.hrconnect.employee.domain.repository.EmployeeRepository;
import com.hrconnect.employee.infrastructure.external.SecuValidationException;
import com.hrconnect.employee.infrastructure.external.SecuValidatorClient;
import com.hrconnect.employee.infrastructure.external.SecuVerificationResponse;
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
    private final SecuValidatorClient secuValidatorClient;

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
     *
     * La vérification du numéro de sécurité sociale est effectuée AVANT la création.
     * En cas de service externe indisponible, le fallback accepte la création (mode dégradé).
     */
    @Transactional
    public Employee createEmployee(Employee employee) {
        log.info("Creating employee: {}", employee.getReference());

        if (employeeRepository.existsByReference(employee.getReference())) {
            throw new IllegalArgumentException("Employee already exists: " + employee.getReference());
        }

        // Vérification du numéro de sécurité sociale auprès du service externe
        // Le CircuitBreaker gère les pannes, le fallback accepte en mode dégradé
        SecuVerificationResponse verificationResponse = secuValidatorClient.verify(
                employee.getNumeroSecuriteSociale(),
                employee.getNom(),
                employee.getPrenom(),
                employee.getDateNaissance()
        );

        if (!verificationResponse.isValid() && !"FALLBACK_MODE".equals(verificationResponse.getErrorCode())) {
            log.warn("Numéro de sécurité sociale invalide: {}", verificationResponse.getMessage());
            throw new SecuValidationException(
                    "Numéro de sécurité sociale invalide: " + verificationResponse.getMessage(),
                    verificationResponse.getErrorCode()
            );
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

