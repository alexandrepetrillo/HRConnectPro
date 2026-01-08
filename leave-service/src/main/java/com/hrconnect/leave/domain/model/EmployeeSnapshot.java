package com.hrconnect.leave.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Projection locale des employés (snapshot)
 * Stocke les informations nécessaires depuis employee.state
 */
@Entity
@Table(name = "employee_snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSnapshot {

    @Id
    private String employeeId;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String email;

    private String role;

    private String departement;

    private String managerId;

    @Column(nullable = false)
    private Double salaireAnnuelBase;

    @Column(nullable = false)
    private String lastEventId;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}

