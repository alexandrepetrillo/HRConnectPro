package com.hrconnect.interview.domain.model;

/**
 * Statuts d'un entretien
 */
public enum InterviewStatus {
    PLANIFIE,   // Entretien planifié
    REALISE,    // Entretien effectué, en attente de validation
    VALIDE,     // Entretien validé (augmentation applicable)
    ANNULE      // Entretien annulé
}
