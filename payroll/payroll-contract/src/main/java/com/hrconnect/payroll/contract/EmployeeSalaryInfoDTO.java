package com.hrconnect.payroll.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les informations de synthèse d'un employé côté paie.
 *
 * Ce DTO est partagé via payroll-contract pour permettre à d'autres services
 * de consommer les données salariales.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSalaryInfoDTO {

    private String employeeId;
    private String reference;
    private String nom;
    private String email;
    private String departement;

    /**
     * Salaire annuel de base (depuis Employee-Service)
     */
    private Double salaireBase;

    /**
     * Total des augmentations accordées
     */
    private Double totalAugmentations;

    /**
     * Salaire annuel actuel (base + augmentations)
     */
    private Double salaireActuel;

    /**
     * Salaire mensuel brut (sans déductions)
     */
    private Double salaireMensuelBrut;

    /**
     * Nombre d'augmentations reçues
     */
    private Integer nombreAugmentations;
}
