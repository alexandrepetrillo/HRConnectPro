package com.hrconnect.leave.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité pour stocker les compteurs de congés restants pour chaque employé
 */
@Entity
@Table(name = "employee_leave_balances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeLeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String employeeId;

    @Column(nullable = false)
    private Integer cpRestants; // Congés payés restants

    @Column(nullable = false)
    private Integer rttRestants; // RTT restants

    @Column(nullable = false)
    private Integer cpAnnuels; // Congés payés annuels totaux

    @Column(nullable = false)
    private Integer rttAnnuels; // RTT annuels totaux

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
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
