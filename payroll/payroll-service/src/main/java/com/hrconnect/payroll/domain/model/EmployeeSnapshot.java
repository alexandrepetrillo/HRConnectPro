package com.hrconnect.payroll.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Projection locale des employés maintenue par synchronisation Kafka
 * Stocke uniquement les informations nécessaires au payroll-service (id et salaire)
 */
@Entity
@Table(name = "employee_snapshot", schema = "payroll")
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

    /**
     * Salaire annuel de base de l'employé
     */
    @Column(name = "salaire_annuel_base")
    private BigDecimal salaireAnnuelBase;

    /**
     * ID du dernier événement traité (pour l'idempotence)
     */
    @Column(name = "last_event_id", nullable = false)
    private String lastEventId;

    /**
     * Timestamp du dernier événement traité
     */
    @Column(name = "last_event_timestamp", nullable = false)
    private Instant lastEventTimestamp;

    /**
     * Version de l'événement
     */
    @Column(name = "event_version", nullable = false)
    private Long eventVersion;
}

