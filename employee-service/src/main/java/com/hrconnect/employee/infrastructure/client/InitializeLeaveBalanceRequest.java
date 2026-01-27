package com.hrconnect.employee.infrastructure.client;

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
    private Integer cpAnnuels;
    private Integer rttAnnuels;
}
