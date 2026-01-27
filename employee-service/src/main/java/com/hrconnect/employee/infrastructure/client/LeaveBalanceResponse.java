package com.hrconnect.employee.infrastructure.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réponse des compteurs de congés
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceResponse {

    private String employeeId;
    private Integer cpRestants;
    private Integer rttRestants;
    private Integer cpAnnuels;
    private Integer rttAnnuels;
}
