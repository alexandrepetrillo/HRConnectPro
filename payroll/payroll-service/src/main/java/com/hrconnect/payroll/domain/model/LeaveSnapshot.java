package com.hrconnect.payroll.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Projection locale des congés (snapshot).
 *
 * RÈGLE D'ARCHITECTURE : Cette entité contient UNIQUEMENT les données
 * provenant du microservice Leave (via l'événement leave.state).
 *
 * Utilisé pour calculer les déductions de congés sans solde sur la paie.
 */
@Entity
@Table(name = "leave_snapshots", schema = "payroll")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveSnapshot {

    @Id
    private String id;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @Column(nullable = false)
    private String type;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(nullable = false)
    private String statut;

    @Column(name = "jours_poses", nullable = false)
    private Integer joursPoses;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    /**
     * Vérifie si ce congé est un congé sans solde approuvé
     */
    public boolean isCongesSansSoldeApprouve() {
        return "SANS_SOLDE".equals(type) && "APPROUVE".equals(statut);
    }

    /**
     * Vérifie si ce congé impacte une période donnée
     */
    public boolean impactsPeriod(LocalDate periodStart, LocalDate periodEnd) {
        return !dateDebut.isAfter(periodEnd) && !dateFin.isBefore(periodStart);
    }
}
