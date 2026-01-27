package com.hrconnect.leave.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour l'initialisation des compteurs de congés
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitializeLeaveBalanceRequest {

    private String employeeId;
    private Integer cpAnnuels;  // Nombre de CP annuels (par défaut 25)
    private Integer rttAnnuels; // Nombre de RTT annuels (par défaut 10)
}
