package com.hrconnect.leave.infrastructure.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Événement snapshot Employee reçu depuis Kafka (topic: employee.state)
 *
 * Cette classe est une copie de EmployeeState du employee-service
 * pour maintenir le découplage entre les services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeStateEvent {

    private String reference;
    private String nom;
    private String email;
    private String telephone;
    private String role;
    private String departement;
    private String managerId;
    private ContratSnapshot contrat;
    private Double salaireAnnuelBase;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContratSnapshot {
        private String type;
        private String debut;
        private String fin;
    }
}

