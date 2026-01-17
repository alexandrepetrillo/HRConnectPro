package com.hrconnect.employee.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO pour la création/mise à jour d'un employé
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {

    @NotBlank(message = "La référence est obligatoire")
    @Size(max = 50, message = "La référence ne doit pas dépasser 50 caractères")
    private String reference;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 200, message = "Le nom ne doit pas dépasser 200 caractères")
    private String nom;

    @NotBlank(message = "Le numéro de sécurité sociale est obligatoire")
    @Pattern(regexp = "^[12][0-9]{14}$", message = "Le numéro de sécurité sociale doit contenir 15 chiffres et commencer par 1 ou 2")
    private String numeroSecuriteSociale;

    @NotNull(message = "La date de naissance est obligatoire")
    private LocalDate dateNaissance;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    @Size(max = 200, message = "L'email ne doit pas dépasser 200 caractères")
    private String email;

    @Size(max = 20, message = "Le téléphone ne doit pas dépasser 20 caractères")
    private String telephone;

    @NotBlank(message = "Le rôle est obligatoire")
    @Size(max = 100, message = "Le rôle ne doit pas dépasser 100 caractères")
    private String role;

    @NotBlank(message = "Le département est obligatoire")
    @Size(max = 100, message = "Le département ne doit pas dépasser 100 caractères")
    private String departement;

    @Size(max = 50, message = "L'identifiant du manager ne doit pas dépasser 50 caractères")
    private String managerId;

    @Valid
    @NotNull(message = "Le contrat est obligatoire")
    private ContratDTO contrat;

    @NotNull(message = "Le salaire annuel de base est obligatoire")
    @Positive(message = "Le salaire annuel de base doit être positif")
    private Double salaireAnnuelBase;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContratDTO {

        @NotBlank(message = "Le type de contrat est obligatoire")
        @Size(max = 50, message = "Le type de contrat ne doit pas dépasser 50 caractères")
        private String type;

        @NotNull(message = "La date de début est obligatoire")
        private LocalDate debut;

        private LocalDate fin;
    }
}

