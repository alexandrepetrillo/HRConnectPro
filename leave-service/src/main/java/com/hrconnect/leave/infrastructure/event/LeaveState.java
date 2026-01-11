package com.hrconnect.leave.infrastructure.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Événement snapshot Leave publié sur Kafka (topic: leave.state)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveState {

    private String id;
    private String employeeId;
    private String type;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String statut;
    private Integer joursPoses;
    private Integer joursTravaillesMois;
    private Integer joursPosesMois;
    private String commentaire;
}

