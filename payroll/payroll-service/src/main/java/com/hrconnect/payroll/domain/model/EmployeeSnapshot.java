package com.hrconnect.payroll.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Projection locale des employés (snapshot).
 *
 * RÈGLE D'ARCHITECTURE : Cette entité contient UNIQUEMENT les données
 * provenant du microservice Employee (via l'événement employee.state).
 * Les données propres à Payroll-Service sont dans des entités séparées.
 */
@Entity
@Table(name = "employee_snapshots", schema = "payroll")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSnapshot {

    @Id
    @Column(name = "employee_id")
    private String employeeId;

    @Column(nullable = false)
    private String reference;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String email;

    /**
     * ⚠️ BUG VOLONTAIRE : Le dev a mis nullable=false en pensant que le téléphone
     * est toujours renseigné. Ça marchait... jusqu'à ce qu'un employé soit créé sans téléphone !
     *
     * Quand un employé sans téléphone arrive → ConstraintViolationException → DLQ
     */
    @Column(nullable = false)  // ❌ BUG : devrait être nullable=true
    private String telephone;

    private String role;

    private String departement;

    @Column(name = "manager_id")
    private String managerId;

    @Column(name = "contrat_type")
    private String contratType;

    @Column(name = "contrat_debut")
    private LocalDate contratDebut;

    @Column(name = "contrat_fin")
    private LocalDate contratFin;

    @Column(name = "salaire_annuel_base", nullable = false)
    private Double salaireAnnuelBase;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
