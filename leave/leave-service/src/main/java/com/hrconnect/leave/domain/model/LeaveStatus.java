package com.hrconnect.leave.domain.model;

/**
 * Statut d'un congé
 */
public enum LeaveStatus {
    EN_ATTENTE,  // En attente de validation
    VALIDE,      // Validé
    REFUSE,      // Refusé
    ANNULE       // Annulé
}

