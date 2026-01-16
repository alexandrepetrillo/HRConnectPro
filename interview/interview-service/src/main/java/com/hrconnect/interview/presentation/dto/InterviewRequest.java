package com.hrconnect.interview.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRequest {

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    @NotBlank(message = "Interview type is required")
    private String type;  // ANNUEL, PROFESSIONNEL, CARRIERE

    @NotNull(message = "Interview date is required")
    private LocalDate dateEntretien;

    private String feedback;

    private Double augmentationAccordee;
}
