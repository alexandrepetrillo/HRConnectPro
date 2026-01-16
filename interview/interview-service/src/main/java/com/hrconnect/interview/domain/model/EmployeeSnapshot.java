package com.hrconnect.interview.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Snapshot local d'un employé (projection depuis employee.state)
 * Contient UNIQUEMENT les données externes provenant d'Employee-Service
 */
@Entity
@Table(name = "employee_snapshots", schema = "interview")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false, unique = true, length = 50)
    private String employeeId;

    @Column(nullable = false)
    private String nom;

    private String email;

    private String departement;

    @Column(name = "manager_id")
    private String managerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
