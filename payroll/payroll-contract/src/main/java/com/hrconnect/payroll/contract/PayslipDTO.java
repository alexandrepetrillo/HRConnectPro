package com.hrconnect.payroll.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO représentant les données nécessaires pour éditer une fiche de paie.
 * Agrège les informations de Employee, Leave et Interview.
 *
 * Ce DTO est partagé via payroll-contract pour permettre à d'autres services
 * de consommer les données de paie.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayslipDTO {

    /**
     * Informations de l'employé
     */
    private EmployeeInfo employee;

    /**
     * Période de paie (format YYYY-MM)
     */
    private String period;

    /**
     * Salaire annuel de base (depuis Employee-Service)
     */
    private Double salaireBase;

    /**
     * Liste des augmentations accordées (depuis Interview-Service)
     */
    private List<AugmentationInfo> augmentations;

    /**
     * Total des augmentations
     */
    private Double totalAugmentations;

    /**
     * Salaire actuel = salaire de base + augmentations
     */
    private Double salaireActuel;

    /**
     * Liste des congés sans solde sur la période (depuis Leave-Service)
     */
    private List<CongesSansSoldeInfo> congesSansSolde;

    /**
     * Nombre total de jours de congés sans solde
     */
    private Integer totalJoursCongesSansSolde;

    /**
     * Déduction pour congés sans solde
     */
    private Double deductionCongesSansSolde;

    /**
     * Salaire brut mensuel final
     */
    private Double salaireBrutMensuel;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeInfo {
        private String reference;
        private String nom;
        private String email;
        private String telephone;
        private String departement;
        private String role;
        private String contratType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AugmentationInfo {
        private String date;
        private Double montant;
        private String type; // Type d'entretien (ANNUEL, PROFESSIONNEL, etc.)
        private String reference; // Référence de l'entretien
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CongesSansSoldeInfo {
        private String dateDebut;
        private String dateFin;
        private Integer jours;
    }
}
