package com.hrconnect.payroll.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Historique des fiches de paie générées.
 *
 * RÈGLE D'ARCHITECTURE : Cette entité contient les données PROPRES à Payroll-Service.
 * C'est le résultat du calcul de paie basé sur les snapshots des autres services.
 */
@Entity
@Table(name = "payslip_history", schema = "payroll")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayslipHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    /**
     * Période de paie au format YYYY-MM
     */
    @Column(name = "period_month", nullable = false)
    private String periodMonth;

    /**
     * Salaire annuel de base (depuis EmployeeSnapshot)
     */
    @Column(name = "salaire_base", nullable = false)
    private Double salaireBase;

    /**
     * Total des augmentations accordées (depuis InterviewSnapshots)
     */
    @Column(name = "total_augmentations", nullable = false)
    @Builder.Default
    private Double totalAugmentations = 0.0;

    /**
     * Salaire actuel = salaire de base + augmentations
     */
    @Column(name = "salaire_actuel", nullable = false)
    private Double salaireActuel;

    /**
     * Nombre de jours de congés sans solde sur la période
     */
    @Column(name = "jours_conges_sans_solde", nullable = false)
    @Builder.Default
    private Integer joursCongesSansSolde = 0;

    /**
     * Montant de la déduction pour congés sans solde
     */
    @Column(name = "deduction_conges_sans_solde", nullable = false)
    @Builder.Default
    private Double deductionCongesSansSolde = 0.0;

    /**
     * Salaire brut mensuel final
     */
    @Column(name = "salaire_brut_mensuel", nullable = false)
    private Double salaireBrutMensuel;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    protected void onCreate() {
        generatedAt = LocalDateTime.now();
    }
}
