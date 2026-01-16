package com.hrconnect.interview.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Événement snapshot Interview publié sur Kafka (topic: interview.state)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewState {

    private String reference;
    private String employeeId;
    private String dateEntretien;
    private String type;              // ANNUEL, PROFESSIONNEL, CARRIERE
    private String feedback;
    private Double augmentationAccordee;
    private String statut;            // PLANIFIE, REALISE, VALIDE, ANNULE
}
