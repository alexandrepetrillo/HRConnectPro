package com.hrconnect.employee.application.service;

import com.hrconnect.employee.contract.EmployeeState;
import com.hrconnect.employee.domain.model.Employee;
import com.hrconnect.employee.domain.repository.EmployeeRepository;
import com.hrconnect.employee.infrastructure.external.SecuValidationException;
import com.hrconnect.employee.infrastructure.external.SecuValidatorClient;
import com.hrconnect.employee.infrastructure.external.SecuVerificationResponse;
import com.hrconnect.socle.kafka.KafkaEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service métier pour la gestion des employés.
 * Publie les événements sur Kafka après commit de la transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {

    private static final String EMPLOYEE_TOPIC = "employee.state";

    private final EmployeeRepository employeeRepository;
    private final KafkaEventPublisher<EmployeeState> kafkaPublisher;
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

        // Publication sur Kafka après commit de la transaction
        kafkaPublisher.publishAfterCommit(EMPLOYEE_TOPIC, saved.getReference(), () -> buildState(saved));

        log.info("Employee created: {}", saved.getReference());
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

        // Publication sur Kafka après commit de la transaction
        kafkaPublisher.publishAfterCommit(EMPLOYEE_TOPIC, updated.getReference(), () -> buildState(updated));

        log.info("Employee updated: {}", updated.getReference());
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

    /**
     * Construit l'état (DTO) à publier sur Kafka.
     */
    private EmployeeState buildState(Employee employee) {
        return EmployeeState.builder()
            .reference(employee.getReference())
            .nom(employee.getNom())
            .prenom(employee.getPrenom())
            .numeroSecuriteSociale(employee.getNumeroSecuriteSociale())
            .dateNaissance(employee.getDateNaissance() != null ? employee.getDateNaissance().toString() : null)
            .email(employee.getEmail())
            .telephone(employee.getTelephone())
            .role(employee.getRole())
            .departement(employee.getDepartement())
            .managerId(employee.getManagerId())
            .contrat(employee.getContrat() != null ? EmployeeState.ContratState.builder()
                .type(employee.getContrat().getType())
                .debut(employee.getContrat().getDebut() != null ? employee.getContrat().getDebut().toString() : null)
                .fin(employee.getContrat().getFin() != null ? employee.getContrat().getFin().toString() : null)
                .build() : null)
            .salaireAnnuelBase(employee.getSalaireAnnuelBase())
            .build();
    }
}

