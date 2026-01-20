package com.hrconnect.employee.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrconnect.employee.contract.EmployeeState;
import com.hrconnect.employee.domain.model.Employee;
import com.hrconnect.socle.outbox.AbstractOutboxService;
import org.springframework.stereotype.Service;

/**
 * Service pour écrire dans la table Outbox.
 * Utilise le socle générique AbstractOutboxService.
 */
@Service
public class OutboxService extends AbstractOutboxService<EmployeeOutboxEvent, Employee, EmployeeState> {

    public OutboxService(EmployeeOutboxEventRepository repository, ObjectMapper objectMapper) {
        super(repository, objectMapper);
    }

    @Override
    protected String getAggregateType() {
        return "Employee";
    }

    @Override
    protected String getAggregateId(Employee employee) {
        return employee.getReference();
    }

    @Override
    protected EmployeeState buildState(Employee employee) {
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

    @Override
    protected EmployeeOutboxEvent createOutboxEvent() {
        return new EmployeeOutboxEvent();
    }

    /**
     * Méthode de compatibilité avec l'ancien code.
     * @deprecated Utiliser {@link #saveState(Employee)} à la place
     */
    @Deprecated
    public void saveEmployeeState(Employee employee) {
        saveState(employee);
    }
}


