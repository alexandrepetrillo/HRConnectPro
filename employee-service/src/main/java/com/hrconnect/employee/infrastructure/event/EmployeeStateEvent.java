package com.hrconnect.employee.infrastructure.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Événement snapshot Employee publié sur Kafka (topic: employee.state)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeStateEvent {

    private String eventId;
    private Instant timestamp;
    private Long version;
    private String source;
    private EmployeeSnapshot employee;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeSnapshot {
        private String reference;
        private String nom;
        private String email;
        private String telephone;
        private String role;
        private String departement;
        private String managerId;
        private ContratSnapshot contrat;
        private Double salaireAnnuelBase;
    }

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

