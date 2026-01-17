package com.hrconnect.employee.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Événement snapshot Employee publié sur Kafka (topic: employee.state)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeState {

    private String reference;
    private String nom;
    private String prenom;
    private String numeroSecuriteSociale;
    private String dateNaissance;
    private String email;
    private String telephone;
    private String role;
    private String departement;
    private String managerId;
    private ContratState contrat;
    private Double salaireAnnuelBase;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContratState {
        private String type;
        private String debut;
        private String fin;
    }
}

