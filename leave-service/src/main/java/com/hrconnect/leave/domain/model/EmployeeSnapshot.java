package com.hrconnect.leave.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Projection locale des employés maintenue par synchronisation Kafka
 * Stocke uniquement les informations nécessaires au leave-service
 */
@Entity
@Table(name = "employee_snapshot", schema = "leave")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence unique de l'employé (clé fonctionnelle)
     */
    @Column(unique = true, nullable = false)
    private String reference;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String email;

    private String telephone;

    private String role;

    private String departement;

    private String managerId;

    /**
     * Type de contrat (CDI, CDD, etc.)
     */
    private String contratType;

    private LocalDate contratDebut;

    private LocalDate contratFin;

    private Double salaireAnnuelBase;

    /**
     * ID du dernier événement traité (pour l'idempotence)
     */
    @Column(nullable = false)
    private String lastEventId;

    /**
     * Timestamp du dernier événement traité
     */
    @Column(nullable = false)
    private Instant lastEventTimestamp;

    /**
     * Version de l'événement
     */
    @Column(nullable = false)
    private Long eventVersion;

    /**
     * Date de dernière mise à jour du snapshot
     */
    @Column(nullable = false)
    private Instant updatedAt;

    @PreUpdate
    @PrePersist
    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }
}
