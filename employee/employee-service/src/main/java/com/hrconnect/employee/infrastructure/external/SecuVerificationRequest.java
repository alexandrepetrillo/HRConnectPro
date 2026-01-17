package com.hrconnect.employee.infrastructure.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO pour la requête de vérification du numéro de sécurité sociale
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecuVerificationRequest {
    private String numeroSecuriteSociale;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
}
