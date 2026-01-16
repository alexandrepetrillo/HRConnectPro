package com.hrconnect.payroll.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Projection locale des entretiens (snapshot).
 *
 * RÈGLE D'ARCHITECTURE : Cette entité contient UNIQUEMENT les données
 * provenant du microservice Interview (via l'événement interview.state).
 *
 * Utilisé pour calculer les augmentations à appliquer sur le salaire.
 */
@Entity
@Table(name = "interview_snapshots", schema = "payroll")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSnapshot {

    @Id
    private String reference;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @Column(name = "date_entretien", nullable = false)
    private LocalDate dateEntretien;

    @Column(nullable = false)
    private String type;

    @Column(name = "augmentation_accordee")
    private Double augmentationAccordee;

    @Column(nullable = false)
    private String statut;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    /**
     * Vérifie si cette augmentation est validée et doit être prise en compte
     */
    public boolean isAugmentationValidee() {
        return "VALIDE".equals(statut) && augmentationAccordee != null && augmentationAccordee > 0;
    }
}
