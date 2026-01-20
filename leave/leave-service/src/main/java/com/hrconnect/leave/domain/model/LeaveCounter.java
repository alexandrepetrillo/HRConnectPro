package com.hrconnect.leave.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Compteurs de congés d'un employé.
 *
 * RÈGLE D'ARCHITECTURE : Cette entité contient les données PROPRES à Leave-Service.
 * Les données provenant d'autres microservices sont dans des entités *Snapshot séparées.
 *
 * Initialisé automatiquement à la réception d'un nouvel employé via employee.state.
 *
 * @see EmployeeSnapshot pour les données provenant d'Employee-Service
 */
@Entity
@Table(name = "leave_counters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveCounter {

    @Id
    private String employeeId;

    /**
     * Solde de congés payés (en jours).
     * Initialisé à 25 jours pour un nouvel employé.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer soldeCP = 25;

    /**
     * Solde de RTT (en jours).
     * Initialisé à 12 jours pour un nouvel employé.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer soldeRTT = 12;

    /**
     * Date de dernière mise à jour du compteur.
     */
    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    /**
     * Décrémente le solde CP.
     * @param jours nombre de jours à décrémenter
     * @throws IllegalArgumentException si le solde est insuffisant
     */
    public void decrementerCP(int jours) {
        if (this.soldeCP < jours) {
            throw new IllegalArgumentException("Solde CP insuffisant: " + this.soldeCP + " < " + jours);
        }
        this.soldeCP -= jours;
    }

    /**
     * Décrémente le solde RTT.
     * @param jours nombre de jours à décrémenter
     * @throws IllegalArgumentException si le solde est insuffisant
     */
    public void decrementerRTT(int jours) {
        if (this.soldeRTT < jours) {
            throw new IllegalArgumentException("Solde RTT insuffisant: " + this.soldeRTT + " < " + jours);
        }
        this.soldeRTT -= jours;
    }
}

