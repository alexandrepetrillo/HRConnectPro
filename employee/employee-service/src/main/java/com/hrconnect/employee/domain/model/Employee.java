package com.hrconnect.employee.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Entité Employé
 */
@Entity
@Table(name = "employees")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employee_seq")
    @SequenceGenerator(name = "employee_seq", sequenceName = "employee_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(nullable = false, unique = true, length = 50, name = "reference")
    private String reference;

    @Column(nullable = false, length = 200)
    private String nom;

    @Column(nullable = false, unique = true, length = 15, name = "numero_securite_sociale")
    private String numeroSecuriteSociale;

    @Column(nullable = false, name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(nullable = false, unique = true, length = 200)
    private String email;

    @Column(length = 20)
    private String telephone;

    @Column(nullable = false, length = 100)
    private String role;

    @Column(nullable = false, length = 100)
    private String departement;

    @Column(length = 50)
    private String managerId;

    @Embedded
    private Contrat contrat;

    @Column(nullable = false)
    private Double salaireAnnuelBase;

    @Version
    private Long version;

    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Contrat {

        @Column(nullable = false, length = 50)
        private String type; // CDI, CDD, Stage, etc.

        @Column(nullable = false)
        private LocalDate debut;

        private LocalDate fin; // null pour CDI
    }
}